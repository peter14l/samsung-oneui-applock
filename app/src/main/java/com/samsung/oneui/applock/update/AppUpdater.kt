package com.samsung.oneui.applock.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.samsung.oneui.applock.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val tag: String,
    val releaseNotes: String,
    val apkUrl: String,
    val abi: String
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class PermissionRequired(val info: UpdateInfo) : UpdateState()
    data class Downloading(
        val info: UpdateInfo,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class Installing(val info: UpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object AppUpdater {

    private const val TAG = "AppUpdater"
    const val R2_BASE_URL = "https://pub-3efdf98689b640149aa277ccc5daea43.r2.dev"
    private const val VERSION_MANIFEST_URL = "$R2_BASE_URL/version.json"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getInstallUnknownAppsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$VERSION_MANIFEST_URL?t=${System.currentTimeMillis()}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Cache-Control", "no-cache")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext UpdateState.Error("Update server returned HTTP $code")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "")
            val tag = json.optString("tag", "v$remoteVersionName")
            val releaseNotes = json.optString("releaseNotes", "Bug fixes and performance improvements.")
            val apksJson = json.optJSONObject("apks")

            if (remoteVersionCode <= BuildConfig.VERSION_CODE) {
                return@withContext UpdateState.UpToDate(BuildConfig.VERSION_NAME)
            }

            val (selectedAbi, apkUrl) = selectApkForDevice(apksJson, tag)
            if (apkUrl.isBlank()) {
                return@withContext UpdateState.Error("No compatible APK found for device architecture")
            }

            UpdateState.Available(
                UpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = remoteVersionName,
                    tag = tag,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    abi = selectedAbi
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            UpdateState.Error(e.localizedMessage ?: "Failed to check for updates")
        } finally {
            connection?.disconnect()
        }
    }

    private fun selectApkForDevice(apksJson: JSONObject?, tag: String): Pair<String, String> {
        val supportedAbis = Build.SUPPORTED_ABIS ?: emptyArray()
        if (apksJson != null) {
            for (abi in supportedAbis) {
                val url = apksJson.optString(abi, "")
                if (url.isNotBlank()) {
                    return abi to url
                }
            }
            val universalUrl = apksJson.optString("universal", "")
            if (universalUrl.isNotBlank()) {
                return "universal" to universalUrl
            }
        }

        // Fallback URL construction matching Gradle split APK naming
        val preferredAbi = supportedAbis.firstOrNull {
            it == "arm64-v8a" || it == "armeabi-v7a" || it == "x86_64"
        } ?: "universal"
        return preferredAbi to "$R2_BASE_URL/releases/$tag/app-$preferredAbi-release.apk"
    }

    suspend fun downloadAndInstall(
        context: Context,
        info: UpdateInfo,
        onState: (UpdateState) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!canRequestPackageInstalls(context)) {
            onState(UpdateState.PermissionRequired(info))
            return@withContext
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "app-${info.abi}-${info.tag}.apk")
        var connection: HttpURLConnection? = null

        try {
            onState(UpdateState.Downloading(info, 0, 0L, 0L))

            val url = URL(info.apkUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                onState(UpdateState.Error("Download failed (HTTP $code)"))
                return@withContext
            }

            val totalBytes = connection.contentLengthLong.coerceAtLeast(0L)
            var downloadedBytes = 0L
            var lastReportedPercent = -1
            var lastReportTime = 0L

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        val now = System.currentTimeMillis()
                        if (percent != lastReportedPercent && (now - lastReportTime >= 80L || percent == 100)) {
                            lastReportedPercent = percent
                            lastReportTime = now
                            onState(UpdateState.Downloading(info, percent, downloadedBytes, totalBytes))
                        }
                    }
                    output.flush()
                }
            }

            onState(UpdateState.Installing(info))
            installApkViaPackageInstaller(context, apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download/install failed", e)
            apkFile.delete()
            onState(UpdateState.Error(e.localizedMessage ?: "Failed to download update"))
        } finally {
            connection?.disconnect()
        }
    }

    private fun installApkViaPackageInstaller(context: Context, apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
            }
        }

        val sessionId = packageInstaller.createSession(params)
        packageInstaller.openSession(sessionId).use { session ->
            FileInputStream(apkFile).use { input ->
                session.openWrite("app_update", 0, apkFile.length()).use { out ->
                    val buffer = ByteArray(16_384)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                    }
                    session.fsync(out)
                }
            }

            val intent = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UpdateInstallReceiver.ACTION_INSTALL_STATUS
                putExtra(UpdateInstallReceiver.EXTRA_APK_PATH, apkFile.absolutePath)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                flags
            )
            session.commit(pendingIntent.intentSender)
        }
    }
}
