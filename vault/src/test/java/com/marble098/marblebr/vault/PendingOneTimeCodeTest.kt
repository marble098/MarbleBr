package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.OneTimeCodeHit
import com.marble098.marblebr.core.model.OneTimeCodeSource
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingOneTimeCodeTest {
    private fun code(value: String, time: Long, source: OneTimeCodeSource = OneTimeCodeSource.SMS) =
        OneTimeCodeHit(code = value, confidence = 100, source = source, receivedAtEpochMillis = time)

    @Test
    fun `completing an old fill does not discard a later SMS`() {
        val slot = PendingOneTimeCode()
        val old = code("123456", 100)
        val later = code("654321", 200)
        assertTrue(slot.offer(old))
        assertFalse(slot.offer(old))
        assertTrue(slot.offer(later))
        assertFalse(slot.consumeMatching(old.identity()))
        assertEquals(later, slot.peek())
        assertTrue(slot.consumeMatching(later.identity()))
        assertNull(slot.peek())
    }

    @Test
    fun `same digits from a later message or another source are distinct`() {
        val sms = code("123456", 100)
        val laterSms = code("123456", 200)
        val notification = code("123456", 100, OneTimeCodeSource.NOTIFICATION)
        val flow = MutableStateFlow<OneTimeCodeHit?>(notification)
        assertFalse(flow.consumeMatching(sms.identity()))
        assertEquals(notification, flow.value)
        flow.value = laterSms
        assertFalse(flow.consumeMatching(sms.identity()))
        assertEquals(laterSms, flow.value)
        assertTrue(flow.consumeMatching(laterSms.identity()))
        assertNull(flow.value)
    }
}
