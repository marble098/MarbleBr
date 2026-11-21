package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های قالب و ادغامِ بستهٔ پشتیبان (بدون رمزنگاری؛ آن بخش در ماژول vault تست می‌شود).
 */
class VaultBackupTest {

    private val header = VaultBackupHeader(
        createdAtEpochMillis = 1_700_000_000_000L,
        saltBase64 = "c2FsdHNhbHRzYWx0c2FsdA==",
        ivBase64 = "aXZpdml2aXZpdg==",
        ciphertextSha256 = "abc123",
        kdfCheckBase64 = "check",
        credentialCount = 2,
        sessionCount = 3,
        siteCount = 1,
    )
    private val body = "A".repeat(300)

    @Test
    fun `round trip keeps every header field and the body`() {
        val text = VaultBackupCodec.encode(header, body)
        val parsed = VaultBackupCodec.decode(text)
        assertTrue(parsed is VaultBackupParseResult.Ready)
        val ready = parsed as VaultBackupParseResult.Ready
        assertEquals(header.saltBase64, ready.header.saltBase64)
        assertEquals(header.createdAtEpochMillis, ready.header.createdAtEpochMillis)
        assertEquals(header.credentialCount, ready.header.credentialCount)
        assertEquals(body, ready.ciphertextBase64)
    }

    @Test
    fun `header never contains user data`() {
        val text = VaultBackupCodec.encode(header, body)
        val head = text.substringBefore("---BODY---")
        listOf("password", "username", "secret").forEach { word ->
            assertFalse(head.lowercase().contains(word))
        }
    }

    @Test
    fun `garbage files are rejected with a named reason`() {
        assertEquals(BackupProblem.NOT_A_BACKUP, (VaultBackupCodec.decode("hello") as VaultBackupParseResult.Invalid).reason)
        assertEquals(
            BackupProblem.CORRUPTED,
            (VaultBackupCodec.decode("MARBLEBR-VAULT-BACKUP\n---BODY---\n") as VaultBackupParseResult.Invalid).reason,
        )
        assertEquals(
            BackupProblem.CORRUPTED,
            (VaultBackupCodec.decode("MARBLEBR-VAULT-BACKUP\nformat=1\n---BODY---\n!!!!") as VaultBackupParseResult.Invalid).reason,
        )
        assertEquals(
            BackupProblem.UNSUPPORTED_VERSION,
            (
                VaultBackupCodec.decode(
                    "MARBLEBR-VAULT-BACKUP\nformat=99\nsalt=c2FsdA==\niv=aXZpdg==\n---BODY---\nQUJD",
                ) as VaultBackupParseResult.Invalid
            ).reason,
        )
    }

    @Test
    fun `file names and password checks are stable`() {
        val name = VaultBackupFormat.suggestedFileName(1_700_000_000_000L)
        assertTrue(name.startsWith("marblebr-backup-"))
        assertTrue(name.endsWith(".mbrx"))
        assertTrue(VaultBackupFormat.looksLikeBackup(name))
        assertFalse(VaultBackupFormat.looksLikeBackup("photo.jpg"))

        val salt = "c2FsdHNhbHRzYWx0c2FsdA=="
        val first = VaultBackupCodec.passwordCheck(salt, 150_000, "correct-horse")
        val second = VaultBackupCodec.passwordCheck(salt, 150_000, "correct-horse")
        val wrong = VaultBackupCodec.passwordCheck(salt, 150_000, "wrong-horse")
        assertEquals(first, second)
        assertFalse(first == wrong)
    }

    @Test
    fun `merging credentials never silently drops existing data`() {
        val existing = listOf(
            VaultBackupCredential("id-1", "https://example.com", "user", "old", updatedAtEpochMillis = 500),
        )
        val newer = VaultBackupCredential("incoming", "https://example.com/", "USER", "new", updatedAtEpochMillis = 900)
        val plan = VaultBackupMerger.planCredentials(listOf(newer), existing, overwriteExisting = true)
        assertEquals(1, plan.updates.size)
        assertEquals(0, plan.additions.size)
        // شناسهٔ موجود حفظ می‌شود تا AAD رکورد در دیتابیس نشکند.
        assertEquals("id-1", plan.updates.single().id)

        val stale = newer.copy(updatedAtEpochMillis = 100)
        val stalePlan = VaultBackupMerger.planCredentials(listOf(stale), existing, overwriteExisting = true)
        assertEquals(1, stalePlan.skipped.size)

        val keepOld = VaultBackupMerger.planCredentials(listOf(newer), existing, overwriteExisting = false)
        assertEquals(1, keepOld.skipped.size)
        assertEquals(0, keepOld.updates.size)
    }

    @Test
    fun `duplicates inside one backup are only added once`() {
        val first = VaultBackupCredential("a", "https://example.com", "user", "one", updatedAtEpochMillis = 10)
        val second = first.copy(id = "b", password = "two", updatedAtEpochMillis = 20)
        val plan = VaultBackupMerger.planCredentials(listOf(first, second), emptyList(), overwriteExisting = true)
        assertEquals(1, plan.additions.size)
        assertEquals("a", plan.additions.single().id)
        assertEquals(1, plan.skipped.size)
    }

    @Test
    fun `sessions plan distinguishes new and existing`() {
        val session = VaultBackupSession(
            id = "s1",
            containerId = "c1",
            domain = "example.com",
            capturedAtEpochMillis = 1,
            ciphertextBase64 = "ct",
            ivBase64 = "iv",
            payloadSha256 = "sha",
            cookieCount = 2,
            storageKeyCount = 1,
            isHealthy = true,
            triggerReason = "AUTO",
            label = "auto",
            isPinned = false,
        )
        assertEquals(1, VaultBackupMerger.planSessions(listOf(session), emptySet(), true).additions.size)
        assertEquals(1, VaultBackupMerger.planSessions(listOf(session), setOf("s1"), true).updates.size)
        assertEquals(1, VaultBackupMerger.planSessions(listOf(session), setOf("s1"), false).skipped.size)
        assertEquals(MergeStrategy.ADD, VaultBackupMerger.strategyFor(MergePlan(additions = listOf(session))))
        assertEquals(MergeStrategy.SKIP, VaultBackupMerger.strategyFor(MergePlan<VaultBackupSession>()))
    }

    @Test
    fun `site keys normalize case and trailing slashes`() {
        assertEquals(
            VaultBackupMerger.key("https://Example.com/", "User@Mail.com "),
            VaultBackupMerger.key("https://example.com", "user@mail.com"),
        )
    }
}
