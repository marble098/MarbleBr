package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.CredentialDao
import com.marble098.marblebr.core.database.CredentialEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class CredentialVaultTest {
    private val dao = MemoryCredentials()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val vault = CredentialVault(dao, AesGcmVaultCipher { key })

    @Test
    fun credentialsAreEncryptedInFullAndCanBeEditedAndDeleted() = runTest {
        val fields = CredentialFields("https://my.mci.ir/panel", "user-one", "secret-password", "private-note")
        val saved = vault.save(fields)
        val stored = dao.getById(saved.id)!!
        assertEquals("", stored.payloadSha256)
        for (secret in listOf(fields.site, fields.username, fields.password, fields.notes)) {
            assertFalse(stored.toString().contains(secret))
        }
        assertEquals(fields, vault.observeCredentials().first().credentials.single().fields)
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
        assertEquals("https://example.com", CredentialVault.normalizeSite("https://WWW.Example.COM"))
        // اسکیم‌های خطرناک هنوز رد می‌شوند؛ «هر سایتی» یعنی آدرسِ وب، نه هر رشته‌ای.
        assertFalse(CredentialVault.isValidSite("data:text/html,<script>alert(1)</script>"))
        assertFalse(CredentialVault.isValidSite("file:///etc/passwd"))
        assertFalse(CredentialVault.isValidSite(""))
    }
}
