# ProGuard / R8 Rules for Samsung One UI App Locker

# AndroidX BiometricPrompt preservation
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public void onAuthenticationError(int, java.lang.CharSequence);
    public void onAuthenticationHelp(int, java.lang.CharSequence);
    public void onAuthenticationSucceeded(androidx.biometric.BiometricPrompt$AuthenticationResult);
    public void onAuthenticationFailed();
}
-keep class androidx.biometric.** { *; }

# Jetpack Security Crypto & Google Tink
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Android Accessibility and Notification Services
-keep public class * extends android.accessibilityservice.AccessibilityService {
    public <init>();
    protected void onServiceConnected();
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
}

-keep public class * extends android.service.notification.NotificationListenerService {
    public <init>();
    public void onNotificationPosted(android.service.notification.StatusBarNotification);
    public void onNotificationRemoved(android.service.notification.StatusBarNotification);
}

# Android Activities & BroadcastReceivers
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver

# Keep Data Models & Preferences
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
