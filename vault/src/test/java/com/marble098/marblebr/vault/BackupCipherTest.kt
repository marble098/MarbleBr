package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.VaultBackupCodec
import com.marble098.marblebr.core.model.VaultBackupFormat
import com.marble098.marblebr.core.model.VaultBackupHeader
import com.marble098.marblebr.core.model.VaultBackupParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * امنیتِ بستهٔ پشتیبان: بدونِ گذرواژهٔ درست، حتی یک بایت از داده‌ها خوانده نمی‌شود.
 */
class BackupCipherTest {

    private val payload = """{"credentials":[{"site":"https://example.com","username":"me","password":"s3cret"}]}"""
    private val password = "correct-horse-battery"

    @Test
    fun `sealed body round trips with the right password`() {
        val sealed = BackupCipher.seal(payload, password)
        val opened = BackupCipher.open(sealed.ciphertextBase64, password, sealed.saltBase64, sealed.ivBase64, VaultBackupFormat.KDF_ITERATIONS)
        assertEquals(payload, opened)
        assertTrue(BackupCipher.verify(sealed.ciphertextBase64, sealed.sha256))
        assertFalse(sealed.ciphertextBase64.contains("s3cret"))
    }

    @Test
    fun `wrong password cannot open the body`() {
        val sealed = BackupCipher.seal(payload, password)
        val opened = runCatching {
            BackupCipher.open(sealed.ciphertextBase64, "wrong-password", sealed.saltBase64, sealed.ivBase64, VaultBackupFormat.KDF_ITERATIONS)
        }
        assertTrue(opened.isFailure)
    }

    @Test
    fun `tampered body fails the integrity check`() {
        val sealed = BackupCipher.seal(payload, password)
        val tampered = sealed.ciphertextBase64.dropLast(8) + "AAAAAAAA"
        assertFalse(BackupCipher.verify(tampered, sealed.sha256))
    }

    @Test
    fun `every backup uses a fresh salt and iv`() {
        val first = BackupCipher.seal(payload, password)
        val second = BackupCipher.seal(payload, password)
        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.ivBase64, second.ivBase64)
        assertNotEquals(first.ciphertextBase64, second.ciphertextBase64)
    }

    @Test
    fun `password fingerprint detects typing mistakes without storing the password`() {
        val sealed = BackupCipher.seal(payload, password)
        val check = BackupCipher.passwordCheck(sealed.saltBase64, VaultBackupFormat.KDF_ITERATIONS, password)
        val wrong = BackupCipher.passwordCheck(sealed.saltBase64, VaultBackupFormat.KDF_ITERATIONS, "wrong-password")
        assertNotEquals(check, wrong)
        assertFalse(check.contains(password))
    }

    @Test
    fun `header and body produced together are readable as a backup file`() {
        val sealed = BackupCipher.seal(payload, password)
        val header = VaultBackupHeader(
            createdAtEpochMillis = 1_700_000_000_000L,
            saltBase64 = sealed.saltBase64,
            ivBase64 = sealed.ivBase64,
            ciphertextSha256 = sealed.sha256,
            kdfCheckBase64 = BackupCipher.passwordCheck(sealed.saltBase64, VaultBackupFormat.KDF_ITERATIONS, password),
            credentialCount = 1,
        )
        val text = VaultBackupCodec.encode(header, sealed.ciphertextBase64)
        val parsed = VaultBackupCodec.decode(text) as VaultBackupParseResult.Ready
        assertEquals(sealed.saltBase64, parsed.header.saltBase64)
        assertEquals(1, parsed.header.credentialCount)
        assertFalse(text.contains("s3cret"))
        assertFalse(text.contains("example.com"))
        assertEquals(
            payload,
            BackupCipher.open(parsed.ciphertextBase64, password, parsed.header.saltBase64, parsed.header.ivBase64, parsed.header.iterations),
        )
    }
}
