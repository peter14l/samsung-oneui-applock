package com.samsung.oneui.applock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

class PinSecurityTest {

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testSaltedPinHashingProducesUniqueHashesForDifferentSalts() {
        val pin = "1234"
        val salt1 = generateSalt()
        val salt2 = generateSalt()

        assertNotEquals(salt1, salt2)

        val hash1 = hashPin(pin, salt1)
        val hash2 = hashPin(pin, salt2)

        assertNotEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
    }

    @Test
    fun testConstantTimeVerification() {
        val pin = "5678"
        val salt = generateSalt()
        val storedHash = hashPin(pin, salt)

        val correctInputHash = hashPin("5678", salt)
        val wrongInputHash = hashPin("1234", salt)

        val isCorrectValid = MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            correctInputHash.toByteArray(Charsets.UTF_8)
        )
        val isWrongValid = MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            wrongInputHash.toByteArray(Charsets.UTF_8)
        )

        assertTrue(isCorrectValid)
        assertFalse(isWrongValid)
    }

    @Test
    fun testPinLengthConstraints() {
        fun isValidPinLength(pin: String): Boolean = pin.length in 4..6 && pin.all { it.isDigit() }

        assertFalse(isValidPinLength("12"))
        assertFalse(isValidPinLength("123"))
        assertTrue(isValidPinLength("1234"))
        assertTrue(isValidPinLength("12345"))
        assertTrue(isValidPinLength("123456"))
        assertFalse(isValidPinLength("1234567"))
        assertFalse(isValidPinLength("abcd"))
    }

    @Test
    fun testLockoutRemainingSecondsCalculation() {
        val now = 1000000L
        val lockoutUntil = now + 30000L // 30 seconds

        val remaining = (lockoutUntil - now + 999) / 1000
        assertEquals(30L, remaining)

        val expiredLockout = now - 5000L
        val expiredRemaining = if (expiredLockout > now) (expiredLockout - now + 999) / 1000 else 0L
        assertEquals(0L, expiredRemaining)
    }

    @Test
    fun testLockoutDurationsByAttemptCount() {
        fun getLockoutDurationMs(attempts: Int): Long = when {
            attempts >= 10 -> 60_000L
            attempts >= 5 -> 30_000L
            else -> 0L
        }

        assertEquals(0L, getLockoutDurationMs(0))
        assertEquals(0L, getLockoutDurationMs(1))
        assertEquals(0L, getLockoutDurationMs(4))
        assertEquals(30_000L, getLockoutDurationMs(5))
        assertEquals(30_000L, getLockoutDurationMs(9))
        assertEquals(60_000L, getLockoutDurationMs(10))
        assertEquals(60_000L, getLockoutDurationMs(15))
    }
}
