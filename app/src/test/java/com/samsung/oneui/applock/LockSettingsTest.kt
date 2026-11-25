package com.samsung.oneui.applock

import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockSettingsTest {

    @Test
    fun testDefaultSettings() {
        val settings = LockSettings()
        assertTrue(settings.masterLockEnabled)
        assertEquals(InterceptionEngine.ACCESSIBILITY, settings.interceptionEngine)
        assertTrue(settings.masterNotificationMasking)
        assertEquals(NotificationMaskMode.GLOBAL, settings.notificationMaskMode)
        assertEquals(RelockPolicy.SCREEN_OFF, settings.relockPolicy)
        assertEquals(4, settings.customPinLength)
        assertEquals(LockDesignStyle.DYNAMIC_THUMB, settings.lockDesignStyle)
    }

    @Test
    fun testSettingsCopy() {
        val original = LockSettings()
        val modified = original.copy(
            masterLockEnabled = false,
            interceptionEngine = InterceptionEngine.USAGE_ACCESS,
            relockPolicy = RelockPolicy.IMMEDIATE,
            customPinLength = 6,
            lockDesignStyle = LockDesignStyle.VAULT_DARK
        )
        assertEquals(false, modified.masterLockEnabled)
        assertEquals(InterceptionEngine.USAGE_ACCESS, modified.interceptionEngine)
        assertEquals(RelockPolicy.IMMEDIATE, modified.relockPolicy)
        assertEquals(6, modified.customPinLength)
        assertEquals(LockDesignStyle.VAULT_DARK, modified.lockDesignStyle)
    }

    @Test
    fun testLockDesignStylesMetadata() {
        val styles = LockDesignStyle.entries
        assertEquals(3, styles.size)
        styles.forEach { style ->
            assertFalse(style.displayName.isBlank())
            assertFalse(style.description.isBlank())
            assertFalse(style.displayName.contains("One UI", ignoreCase = true))
            assertFalse(style.displayName.contains("Samsung", ignoreCase = true))
        }
    }
}
