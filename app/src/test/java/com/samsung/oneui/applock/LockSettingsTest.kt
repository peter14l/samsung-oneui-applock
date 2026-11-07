package com.samsung.oneui.applock

import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockSettingsTest {

    @Test
    fun testDefaultSettings() {
        val settings = LockSettings()
        assertTrue(settings.masterLockEnabled)
        assertTrue(settings.masterNotificationMasking)
        assertEquals(NotificationMaskMode.GLOBAL, settings.notificationMaskMode)
        assertEquals(RelockPolicy.SCREEN_OFF, settings.relockPolicy)
    }

    @Test
    fun testSettingsCopy() {
        val original = LockSettings()
        val modified = original.copy(
            masterLockEnabled = false,
            relockPolicy = RelockPolicy.IMMEDIATE
        )
        assertEquals(false, modified.masterLockEnabled)
        assertEquals(RelockPolicy.IMMEDIATE, modified.relockPolicy)
    }
}
