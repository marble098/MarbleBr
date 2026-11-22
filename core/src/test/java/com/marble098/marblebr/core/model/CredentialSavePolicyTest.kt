package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** سیاستِ «پیشنهادِ سریعِ ذخیره»: چه وقت بپرسیم، چه وقت بی‌صدا ذخیره کنیم، چه وقت هیچ. */
class CredentialSavePolicyTest {

    private val now = 1_700_000_000_000L
    private val site = "https://www.example.com/login"

    private fun decide(
        username: String = "user@example.com",
        password: String = "vT7#qLm2!xR9pWs4",
        known: List<KnownAccount> = emptyList(),
        dismissedAt: Long? = null,
        mode: CredentialSaveMode = CredentialSaveMode.ASK,
        isPrivateTab: Boolean = false,
        saveEnabled: Boolean = true,
        originIsWarning: Boolean = false,
        otpOnly: Boolean = false,
    ) = CredentialSavePolicy.decide(
        username = username,
        password = password,
        isPrivateTab = isPrivateTab,
        saveEnabled = saveEnabled,
        mode = mode,
        knownAccounts = known,
        dismissedAtEpochMillis = dismissedAt,
        nowEpochMillis = now,
        originIsWarning = originIsWarning,
        hasOneTimeCodeOnly = otpOnly,
    )

    @Test
    fun `first login on a site offers a prefilled card`() {
        val action = decide()
        assertTrue(action is SaveOfferAction.Offer)
        assertEquals(OfferReason.FIRST_ACCOUNT, (action as SaveOfferAction.Offer).reason)
    }

    @Test
    fun `same account already saved is not offered again`() {
        val known = listOf(KnownAccount("user@example.com", site, now - 1000L))
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.ALREADY_SAVED),
            decide(known = known),
        )
    }

    @Test
    fun `changed password for the same account is offered again`() {
        val known = listOf(
            KnownAccount(
                username = "user@example.com",
                site = site,
                updatedAtEpochMillis = now - 5_000L,
                passwordFingerprint = PasswordFingerprint.of("old-password-123"),
            ),
        )
        val action = decide(known = known)
        assertTrue(action is SaveOfferAction.Offer)
        assertEquals(OfferReason.PASSWORD_CHANGED, (action as SaveOfferAction.Offer).reason)
    }

    @Test
    fun `same password fingerprint suppresses the offer`() {
        val known = listOf(
            KnownAccount(
                username = "user@example.com",
                site = site,
                passwordFingerprint = PasswordFingerprint.of("vT7#qLm2!xR9pWs4"),
            ),
        )
        assertEquals(SaveOfferAction.Suppressed(SuppressReason.ALREADY_SAVED), decide(known = known))
    }

    @Test
    fun `second account on the same site always asks instead of auto saving`() {
        val known = listOf(KnownAccount("other@example.com", site))
        val action = decide(known = known, mode = CredentialSaveMode.AUTO_SAVE)
        assertTrue(action is SaveOfferAction.Offer)
        assertEquals(OfferReason.ANOTHER_ACCOUNT, (action as SaveOfferAction.Offer).reason)
    }

    @Test
    fun `auto save mode stores unambiguous first logins silently`() {
        val action = decide(mode = CredentialSaveMode.AUTO_SAVE)
        assertTrue(action is SaveOfferAction.AutoSave)
        assertEquals(AutoSaveReason.FIRST_ACCOUNT, (action as SaveOfferAction.AutoSave).reason)
    }

    @Test
    fun `disabled save mode never offers anything`() {
        val action = decide(mode = CredentialSaveMode.DISABLED, saveEnabled = false)
        assertEquals(SaveOfferAction.Suppressed(SuppressReason.SAVE_DISABLED), action)
    }

    @Test
    fun `dismissal suppresses the card during the cooldown window`() {
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.RECENTLY_DISMISSED),
            decide(dismissedAt = now - 60_000L),
        )
        val later = decide(dismissedAt = now - CredentialSavePolicy.DISMISS_COOLDOWN_MILLIS - 1)
        assertTrue(later is SaveOfferAction.Offer)
    }

    @Test
    fun `private tabs unsafe origins and otp only submissions are never offered`() {
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.PRIVATE_TAB),
            decide(isPrivateTab = true),
        )
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.UNSAFE_ORIGIN),
            decide(originIsWarning = true),
        )
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.ONE_TIME_CODE_ONLY),
            decide(otpOnly = true),
        )
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.NO_PASSWORD),
            decide(password = ""),
        )
    }

    @Test
    fun `phone numbers in different formats are recognized as one account`() {
        val known = listOf(KnownAccount("09123456789", site))
        assertEquals(
            SaveOfferAction.Suppressed(SuppressReason.ALREADY_SAVED),
            decide(username = "+989123456789", known = known),
        )
        assertEquals(
            CredentialSavePolicy.normalizeUsername("+98 912 345 6789"),
            CredentialSavePolicy.normalizeUsername("09123456789"),
        )
    }

    @Test
    fun `fingerprints are salted per record and hide the password`() {
        val key = "a-key-for-tests-that-is-long".toByteArray()
        val one = PasswordFingerprint.of("same-password", key, salt = "record-1")
        val two = PasswordFingerprint.of("same-password", key, salt = "record-2")
        assertNotEquals(one, two)
        assertEquals(PasswordFingerprint.LENGTH, one.length)
        assertFalse(one.contains("same"))
        // بدونِ کلیدِ درست، اثر انگشت چیزِ مفیدی لو نمی‌دهد.
        assertNotEquals(one, PasswordFingerprint.of("same-password", "another-key-here-1234".toByteArray(), salt = "record-1"))
    }

    @Test
    fun `keystore style mac produces the same fingerprint as raw key`() {
        // مسیرِ واقعیِ اندروید کلیدِ Keystore را مستقیم به Mac می‌دهد (چون encoded آن null است).
        // این آزمون تضمین می‌کند دو مسیر یک نتیجهٔ یکسان بدهند؛ وگرنه «گذرواژه عوض شد» هرگز
        // تشخیص داده نمی‌شد و کارتِ ذخیره بی‌دلیل دوباره ظاهر می‌شد.
        val key = "a-key-for-tests-that-is-long".toByteArray()
        val raw = PasswordFingerprint.of("same-password", key, salt = "record-1")
        val mac = javax.crypto.Mac.getInstance("HmacSHA256").apply {
            init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
        }
        assertEquals(raw, PasswordFingerprint.of("same-password", mac, salt = "record-1"))
    }

    @Test
    fun `empty password never yields a fingerprint`() {
        assertEquals("", PasswordFingerprint.of(""))
        assertEquals(
            "",
            PasswordFingerprint.of("", javax.crypto.Mac.getInstance("HmacSHA256").apply {
                init(javax.crypto.spec.SecretKeySpec("a-key-for-tests-that-is-long".toByteArray(), "HmacSHA256"))
            }),
        )
    }
}
