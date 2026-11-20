package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.WebAppContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountContainersTest {
    @Test
    fun twoAccountsForTheSameSiteHaveIndependentDurableContexts() {
        val first = WebAppContainer(id = "my_mci_ir", domain = "my.mci.ir", displayName = "First")
        val second = AccountContainers.newAccount(first, " Second ", "unique-id")
        assertEquals(first.domain, second.domain)
        assertEquals("Second", second.displayName)
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.contextId, second.contextId)
        assertEquals("my_mci_ir", AccountContainers.defaultId(first.domain))
        assertFalse(second.isPinnedPwa)
    }
}
