package com.marble098.marblebr

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.marble098.marblebr.core.database.MarbleBrDatabase
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.PopupPermissionRequest
import com.marble098.marblebr.engine.LightweightAdBlocker
import com.marble098.marblebr.engine.SessionHealthGuard
import com.marble098.marblebr.engine.VaultRestoreOutcome
import com.marble098.marblebr.vault.AesGcmVaultCipher
import com.marble098.marblebr.vault.CookiePersistenceConverter
import com.marble098.marblebr.vault.SessionVaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import kotlin.concurrent.thread

/** Exercises the actual Gecko cookie jars, content bridge and HttpOnly restore on an emulator. */
@RunWith(AndroidJUnit4::class)
class GeckoAccountIsolationInstrumentedTest {
    @Test
    fun accountsPersistAndRestoreWithoutCrossingContexts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, MarbleBrDatabase::class.java).build()
        val prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = prefsScope) {
            context.preferencesDataStoreFile("test_${UUID.randomUUID()}")
        }
        val prefs = BrowserPreferencesRepository(store)
        val converter = CookiePersistenceConverter()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val vault = SessionVaultManager(db.snapshotDao(), AesGcmVaultCipher { key }, converter)
        val manager = ContainerSessionManager(
            context, db.containerDao(), vault, SessionHealthGuard(db.healthEventDao()),
            LightweightAdBlocker(), LocalizationManager(prefs), prefs,
            // این تست فقط جریانِ نشست/کوکی را می‌سنجد؛ کاوشگرِ ورود لازم نیست.
            javax.inject.Provider { error("LoginWatcher is not used in this test") },
        )

        LocalTestSite().use { site ->
            val first = runBlocking { manager.getOrCreateContainerForUrl(site.origin) }
            val second = runBlocking { manager.createAdditionalAccount(first, "Second") }
            assertNotEquals(first.contextId, second.contextId)
            try {
                open(manager, first, "first", "${site.origin}/login-page?who=first")
                val firstLogin = site.observe("login-first")
                assertEquals("first", firstLogin.identity)
                assertTrue(firstLogin.cookie.contains("sid=first"))
                assertTrue(capture(manager, "first", "${site.origin}/panel"))
                val firstSnapshot = runBlocking { requireNotNull(vault.restoreLatestHealthySnapshot(first.id)) }
                assertTrue(firstSnapshot.cookies.any { it.name == "sid" && it.value == "first" && it.isHttpOnly })

                open(manager, second, "second", "${site.origin}/login-page?who=second")
                val secondLogin = site.observe("login-second")
                assertEquals("second", secondLogin.identity)
                assertTrue(secondLogin.cookie.contains("sid=second"))
                assertTrue(capture(manager, "second", "${site.origin}/panel"))
                val secondSnapshot = runBlocking { db.snapshotDao().getLatestHealthySnapshot(second.id) }
                requireNotNull(secondSnapshot)
                val secondPayload = runBlocking { requireNotNull(vault.restoreSnapshotById(secondSnapshot.id)) }
                assertTrue(secondPayload.cookies.any { it.name == "sid" && it.value == "second" && it.isHttpOnly })

                navigate(manager, "first", "${site.origin}/observe-page?tag=first-check")
                assertAccount(site.observe("first-check"), "first")
                navigate(manager, "second", "${site.origin}/observe-page?tag=second-check")
                assertAccount(site.observe("second-check"), "second")
                assertFalse("A capture for an old route must not read the current document", capture(manager, "second", "${site.origin}/panel"))

                val popupRequests = LinkedBlockingQueue<PopupPermissionRequest>()
                instrumentation.runOnMainSync {
                    manager.getOrCreateGeckoSession(
                        tabId = "popup-parent",
                        container = first,
                        initialUrl = "${site.origin}/popup-parent",
                        adBlockEnabled = false,
                        onPopupRequest = { popupRequests.add(it) },
                    ).setActive(true)
                }
                val popup = requireNotNull(popupRequests.poll(25, TimeUnit.SECONDS)) { "Gecko did not request a popup" }
                assertTrue(popup.shouldPrompt)
                assertTrue(popup.targetUri in setOf("about:blank", "${site.origin}/oauth/authorize"))
                instrumentation.runOnMainSync {
                    popup.respond(
                        manager.getOrCreateGeckoSession(
                            tabId = "popup-child",
                            container = first,
                            initialUrl = popup.targetUri,
                            adBlockEnabled = false,
                            openedByGecko = true,
                        ),
                    )
                }
                assertAccount(site.observe("popup-with-opener"), "first")

                // Simulate Gecko reporting a content process exit, then reopen the same account and URL.
                instrumentation.runOnMainSync {
                    val session = requireNotNull(manager.activeSession("first"))
                    requireNotNull(session.contentDelegate).onKill(session)
                    assertEquals(null, manager.activeSession("first"))
                }
                open(manager, first, "first", "${site.origin}/observe-page?tag=first-reopen")
                assertAccount(site.observe("first-reopen"), "first")

                // Save the current account state, then remove its browser data and test an explicit restore.
                assertTrue(capture(manager, "second", "${site.origin}/observe-page?tag=second-check"))
                val latestSnapshot = runBlocking { requireNotNull(db.snapshotDao().getLatestHealthySnapshot(second.id)) }
                val staleOutcome = runBlocking { manager.restoreSnapshotOnOpenTab(secondSnapshot.toMeta()) }
                assertTrue("An older snapshot must not replace newer state", staleOutcome.superseded)
                navigate(manager, "second", "${site.origin}/clear-page")
                val cleared = site.observe("cleared")
                assertEquals("none", cleared.identity)
                assertFalse(cleared.cookie.contains("sid="))
                val outcome = awaitRestore(manager, latestSnapshot.toMeta())
                val cookieMetadata = secondPayload.cookies.map { it.copy(value = "[redacted]") }
                assertTrue("Cookie restore failed: $outcome; cookies: $cookieMetadata", outcome.cookiesRestored > 0)
                assertTrue("Storage restore failed: $outcome", outcome.storageKeysRestored > 0)
                navigate(manager, "second", "${site.origin}/observe-page?tag=second-restored")
                assertAccount(site.observe("second-restored"), "second")
                navigate(manager, "first", "${site.origin}/observe-page?tag=first-after-second-restore")
                assertAccount(site.observe("first-after-second-restore"), "first")

                runBlocking { withContext(Dispatchers.Main) { manager.signOutNow(first) } }
                navigate(manager, "second", "${site.origin}/observe-page?tag=second-after-first-signout")
                assertAccount(site.observe("second-after-first-signout"), "second")
                open(manager, first, "first", "${site.origin}/observe-page?tag=first-after-signout")
                val signedOut = site.observe("first-after-signout")
                assertEquals("none", signedOut.identity)
                assertFalse(signedOut.cookie.contains("sid="))
            } finally {
                instrumentation.runOnMainSync {
                    manager.closeTabSession("first")
                    manager.closeTabSession("second")
                    manager.closeTabSession("popup-child")
                    manager.closeTabSession("popup-parent")
                }
                db.close()
                prefsScope.cancel()
            }
        }
    }

    private fun open(manager: ContainerSessionManager, account: WebAppContainer, tab: String, url: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            manager.getOrCreateGeckoSession(tab, account, url, adBlockEnabled = false).setActive(true)
        }
    }

    private fun navigate(manager: ContainerSessionManager, tab: String, url: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { manager.loadInOpenSession(tab, url) }
    }

    private fun capture(manager: ContainerSessionManager, tab: String, url: String): Boolean = runBlocking {
        withContext(Dispatchers.Main) { manager.capturePageSnapshot(tab, url, manual = true) }
    }

    private fun awaitRestore(manager: ContainerSessionManager, snapshot: SessionSnapshotMeta): VaultRestoreOutcome = runBlocking {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        var outcome = manager.restoreSnapshotOnOpenTab(snapshot)
        while (outcome.openTabMissing && System.nanoTime() < deadline) {
            delay(200)
            outcome = manager.restoreSnapshotOnOpenTab(snapshot)
        }
        outcome
    }

    private fun assertAccount(observation: Observation, expected: String) {
        assertEquals(expected, observation.identity)
        assertTrue(observation.cookie.contains("sid=$expected"))
    }
}

private data class Observation(val tag: String, val identity: String, val cookie: String)

/** Tiny loopback-only HTTP site; no credentials or test data leave the emulator. */
private class LocalTestSite : Closeable {
    private val server = ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"))
    private val executor = Executors.newCachedThreadPool()
    private val observations = LinkedBlockingQueue<Observation>()
    val origin = "http://127.0.0.1:${server.localPort}"

    init {
        thread(name = "marble-test-site", isDaemon = true) {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: Exception) { break }
                try {
                    executor.execute {
                        try {
                            handle(socket)
                        } catch (_: IOException) {
                            // Gecko may open speculative connections or abort requests on navigation.
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    socket.close()
                }
            }
        }
    }

    fun observe(tag: String): Observation {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(35)
        while (System.nanoTime() < deadline) {
            val value = observations.poll(1, TimeUnit.SECONDS) ?: continue
            if (value.tag == tag) return value
        }
        error("Timed out waiting for Gecko observation: $tag")
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 5_000
            val reader = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            val request = reader.readLine() ?: return
            val target = request.split(" ").getOrNull(1) ?: return
            var cookie = ""
            while (true) {
                val header = reader.readLine() ?: return
                if (header.isEmpty()) break
                if (header.startsWith("Cookie:", true)) cookie = header.substringAfter(':').trim()
            }
            val path = target.substringBefore('?')
            val query = target.substringAfter('?', "").split('&').mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.isEmpty() || parts[0].isBlank()) null else parts[0] to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
            }.toMap()
            if (path == "/observe") {
                observations.offer(Observation(query["tag"].orEmpty(), query["identity"].orEmpty(), cookie))
            }
            val body = when (path) {
                "/login-page" -> loginPage(query["who"].orEmpty())
                "/observe-page" -> observePage(query["tag"].orEmpty())
                "/clear-page" -> clearPage()
                "/popup-parent" -> popupParentPage()
                "/oauth/authorize" -> popupChildPage()
                else -> "<!doctype html><title>MarbleBr test</title>"
            }
            val headers = when (path) {
                "/set" -> "Set-Cookie: sid=${query["who"]}; Path=/; HttpOnly; SameSite=Lax\r\n"
                "/clear" -> "Set-Cookie: sid=; Path=/; HttpOnly; Max-Age=0\r\n"
                else -> ""
            }
            val bytes = body.toByteArray(Charsets.UTF_8)
            val response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Cache-Control: no-store\r\nConnection: close\r\n$headers" +
                "Content-Length: ${bytes.size}\r\n\r\n"
            client.getOutputStream().apply { write(response.toByteArray(Charsets.ISO_8859_1)); write(bytes); flush() }
        }
    }

    private fun loginPage(who: String) = """
        <!doctype html><title>Test sign-in</title><script>
        setTimeout(async () => {
          await fetch('/set?who=$who');
          localStorage.setItem('identity', '$who');
          history.replaceState(null, '', '/panel');
          await fetch('/observe?tag=login-$who&identity=' + localStorage.getItem('identity'));
        }, 2500);
        </script>
    """.trimIndent()

    private fun observePage(tag: String) = """
        <!doctype html><title>Test observation</title><script>
        fetch('/observe?tag=$tag&identity=' + (localStorage.getItem('identity') || 'none'));
        </script>
    """.trimIndent()

    private fun popupParentPage() = """
        <!doctype html><title>Sign-in popup opener</title><script>
        window.addEventListener('message', event => {
          if (event.origin === location.origin && event.data === 'popup-ready') {
            fetch('/observe?tag=popup-with-opener&identity=' + localStorage.getItem('identity'));
          }
        });
        window.open('/oauth/authorize', 'signin');
        </script>
    """.trimIndent()

    private fun popupChildPage() = """
        <!doctype html><title>Sign-in popup</title><script>
        if (localStorage.getItem('identity') === 'first') {
          window.opener?.postMessage('popup-ready', location.origin);
        }
        </script>
    """.trimIndent()

    private fun clearPage() = """
        <!doctype html><title>Test sign-out</title><script>
        (async () => {
          await fetch('/clear');
          localStorage.removeItem('identity');
          localStorage.setItem('marble_test_bridge_marker', 'ready');
          history.replaceState(null, '', '/panel');
          await fetch('/observe?tag=cleared&identity=none');
        })();
        </script>
    """.trimIndent()

    override fun close() {
        server.close()
        executor.shutdownNow()
    }
}
