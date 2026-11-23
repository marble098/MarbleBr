package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.CredentialDao
import com.marble098.marblebr.core.database.CredentialEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class CredentialVaultTest {
    private val dao = MemoryCredentials()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val vault = CredentialVault(dao, AesGcmVaultCipher { key }, FixedFingerprintKey)

    @Test
    fun credentialsAreEncryptedInFullAndCanBeEditedAndDeleted() = runTest {
        val fields = CredentialFields("https://my.mci.ir/panel", "user-one", "secret-password", "private-note")
        val saved = vault.save(fields)
        val stored = dao.getById(saved.id)!!
        assertEquals("", stored.payloadSha256)
        for (secret in listOf(fields.site, fields.username, fields.password, fields.notes)) {
            assertFalse(stored.toString().contains(secret))
        }
        assertEquals(fields.copy(autofillOrigin = "https://my.mci.ir"), vault.observeCredentials().first().credentials.single().fields)
        vault.save(fields.copy(password = "new-password"), saved.id)
        assertEquals("new-password", vault.observeCredentials().first().credentials.single().fields.password)
        assertEquals(saved.createdAtEpochMillis, dao.getById(saved.id)?.createdAtEpochMillis)
        vault.delete(saved.id)
        assertTrue(vault.observeCredentials().first().credentials.isEmpty())
    }

    @Test
    fun movingCiphertextToAnotherIdFailsAuthentication() = runTest {
        val saved = vault.save(CredentialFields("https://example.org", "user", "password"))
        val entry = dao.getById(saved.id)!!
        dao.upsert(entry.copy(id = "different-id"))
        val result = vault.observeCredentials().first()
        assertEquals(listOf(saved.id), result.credentials.map { it.id })
        assertEquals(1, result.unreadableCount)
        assertNotEquals(null, dao.getById("different-id"))
    }

    @Test
    fun usageMetadataSurvivesPasswordUpdates() = runTest {
        // «آخرین استفاده» نباید با ذخیرهٔ گذرواژهٔ تازه صفر شود؛ وگرنه ترتیبِ اولویتِ حساب‌ها
        // (که بر پایهٔ همین زمان است) با هر بار عوض کردنِ گذرواژه می‌پرد.
        val saved = vault.save(CredentialFields("https://example.org", "user", "old-password"))
        assertTrue(vault.touchCredential(saved.id, autofilledAtEpochMillis = 1_000L, oneTimeCodeAtEpochMillis = 2_000L))
        val afterTouch = vault.observeCredentials().first().credentials.single().fields
        assertEquals(1_000L, afterTouch.lastAutofillAtEpochMillis)
        assertEquals(2_000L, afterTouch.lastOneTimeCodeAtEpochMillis)

        vault.saveCaptured("https://example.org", "user", "brand-new-password", saved.id)
        val afterUpdate = vault.observeCredentials().first().credentials.single().fields
        assertEquals("brand-new-password", afterUpdate.password)
        assertEquals(1_000L, afterUpdate.lastAutofillAtEpochMillis)
        assertEquals(2_000L, afterUpdate.lastOneTimeCodeAtEpochMillis)
    }

    @Test
    fun fingerprintIsStablePerRecordAndChangesWithThePassword() = runTest {
        val one = vault.save(CredentialFields("https://example.org", "user", "first-password"))
        val two = vault.save(CredentialFields("https://example.org", "other", "first-password"))
        val firstFingerprint = vault.passwordFingerprint(one.id)
        assertNotEquals(null, firstFingerprint)
        // نمک = شناسهٔ رکورد: دو حساب با گذرواژهٔ یکسان اثر انگشتِ یکسان ندارند.
        assertNotEquals(firstFingerprint, vault.passwordFingerprint(two.id))
        vault.save(CredentialFields("https://example.org", "user", "second-password"), one.id)
        assertNotEquals(firstFingerprint, vault.passwordFingerprint(one.id))
    }

    @Test
    fun siteValidationAndGeneratedPasswords() {
        assertTrue(CredentialVault.isValidSite("https://my.mci.ir/panel"))
        assertFalse(CredentialVault.isValidSite("javascript:alert(1)"))
        assertFalse(CredentialVault.isValidSite("https://example.org@evil.test"))
        val one = StrongPasswordGenerator.generate()
        val two = StrongPasswordGenerator.generate()
        assertEquals(20, one.length)
        assertNotEquals(one, two)
        assertTrue(one.any(Char::isUpperCase))
        assertTrue(one.any(Char::isLowerCase))
        assertTrue(one.any(Char::isDigit))
        assertTrue(one.any { it in "!@#%+-_=" })
    }

    @Test
    fun autofillRequiresTheConfirmedHttpsOriginIncludingHostAndPort() = runTest {
        val saved = vault.save(CredentialFields("https://www.example.org:8443/login", "alice", "secret"))
        assertNotNull(vault.credentialsForAutofill(saved.id, "https://www.example.org:8443/other"))
        assertNull(vault.credentialsForAutofill(saved.id, "https://example.org:8443/other"))
        assertNull(vault.credentialsForAutofill(saved.id, "https://www.example.org/other"))
        assertNull(vault.credentialsForAutofill(saved.id, "https://sub.www.example.org:8443/other"))
        assertNull(vault.credentialsForAutofill(saved.id, "http://www.example.org:8443/other"))
        assertNull(vault.credentialsForAutofill(saved.id, "https://user@www.example.org:8443/other"))

        val defaultPort = vault.save(CredentialFields("example.org", "bob", "password"))
        assertNotNull(vault.credentialsForAutofill(defaultPort.id, "https://example.org:443/"))
        assertNull(vault.credentialsForAutofill(defaultPort.id, "https://www.example.org/"))
    }

    @Test
    fun legacyOriginMustBeExplicitlyReviewedAndMetadataUpdatesDoNotApproveIt() = runTest {
        val id = "legacy"
        val legacyJson = """{"site":"https://example.org","username":"alice","password":"secret"}"""
        val blob = AesGcmVaultCipher { key }.encrypt(legacyJson, "credential:$id", includePlaintextHash = false)
        dao.upsert(CredentialEntity(id, blob.ciphertextBase64, blob.ivBase64, blob.plaintextSha256, 1L, 1L))
        assertNull(vault.credentialsForAutofill(id, "https://example.org/"))
        assertTrue(vault.touchCredential(id, oneTimeCodeAtEpochMillis = 2L))
        assertNull(vault.credentialsForAutofill(id, "https://example.org/"))
        val legacy = requireNotNull(vault.credentialById(id))
        vault.save(legacy.fields.copy(site = "https://www.example.org:8443"), id)
        assertNotNull(vault.credentialsForAutofill(id, "https://www.example.org:8443/login"))
        assertNull(vault.credentialsForAutofill(id, "https://example.org/"))
    }

    @Test
    fun backupPreservesConfirmedOriginAndOldBackupsRemainUnconfirmed() = runTest {
        val saved = vault.save(CredentialFields("https://www.example.org:8443", "alice", "secret"))
        val backup = vault.credentialsForBackup().single()
        assertEquals("https://www.example.org:8443", backup.autofillOrigin)
        vault.delete(saved.id)
        val restored = vault.saveFromBackup(backup)
        assertNotNull(vault.credentialsForAutofill(restored.id, "https://www.example.org:8443/login"))
        vault.delete(restored.id)
        val old = vault.saveFromBackup(backup.copy(autofillOrigin = null))
        assertNull(vault.credentialsForAutofill(old.id, "https://www.example.org:8443/login"))
    }

    /**
     * همان قراردادِ واقعیِ اپ: یک `Mac` آماده با کلیدِ ثابتِ آزمون (در اپ، کلیدِ غیرقابل‌استخراجِ
     * Keystore تزریق می‌شود). بایت‌های کلید این‌جا فقط برای آزمون است.
     */
    private object FixedFingerprintKey : FingerprintKeyProvider {
        override fun fingerprintMac(): javax.crypto.Mac =
            javax.crypto.Mac.getInstance("HmacSHA256").apply {
                init(javax.crypto.spec.SecretKeySpec("vault-test-fingerprint-key-01".toByteArray(), "HmacSHA256"))
            }
    }

    private class MemoryCredentials : CredentialDao {
        private val state = MutableStateFlow<List<CredentialEntity>>(emptyList())
        override fun observeAll() = state
        override suspend fun getById(id: String) = state.value.firstOrNull { it.id == id }
        override suspend fun upsert(credential: CredentialEntity) {
            state.value = state.value.filterNot { it.id == credential.id } + credential
        }
        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }
    }

    @Test
    fun anySiteAddressIsAcceptedAfterNormalization() {
        // «افزودن هر سایتی»: آدرسِ کوتاه که کاربر تایپ می‌کند هم معتبر است.
        assertTrue(CredentialVault.isValidSite("my.mci.ir"))
        assertTrue(CredentialVault.isValidSite("example.com/login?next=/panel"))
        assertTrue(CredentialVault.isValidSite("app.example.ir:8443/sso"))
        assertEquals("https://my.mci.ir/panel", CredentialVault.normalizeSite("my.mci.ir/panel"))
        assertEquals("https://www.example.com", CredentialVault.normalizeSite("https://WWW.Example.COM"))
        assertEquals("https://app.example.ir:8443/sso", CredentialVault.normalizeSite("app.example.ir:8443/sso"))
        // اسکیم‌های خطرناک هنوز رد می‌شوند؛ «هر سایتی» یعنی آدرسِ وب، نه هر رشته‌ای.
        assertFalse(CredentialVault.isValidSite("data:text/html,<script>alert(1)</script>"))
        assertFalse(CredentialVault.isValidSite("file:///etc/passwd"))
        assertFalse(CredentialVault.isValidSite(""))
    }
}
