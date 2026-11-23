/* Run with node; exercise the actual extension with a minimal WebExtension event harness. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const { webcrypto } = require("node:crypto");
const listeners = {};
const event = name => ({ addListener: fn => { listeners[name] = fn; } });
const browser = {
  runtime: { onMessage: event("message") },
  webRequest: { onBeforeRequest: event("before"), onBeforeSendHeaders: event("requestHeaders"), onHeadersReceived: event("headers"), onCompleted: event("completed") },
  tabs: { onRemoved: event("removed"), sendMessage: () => Promise.resolve() }
};
const code = fs.readFileSync("engine/src/main/assets/extensions/never_signout/background.js", "utf8");
vm.runInNewContext(code, { browser, URL, crypto: webcrypto, Uint8Array, Date, Map, Promise, Number, String, Array });
const sender = id => ({ tab: { id }, frameId: 0, url: "https://my.mci.ir/panel" });
let restoreSequence = 0;
const send = (id, msg) => listeners.message(msg.type === "PREPARE_RESTORE" && !msg.restoreId ?
  { ...msg, restoreId: `restore-${++restoreSequence}` } : msg, sender(id));
const headers = (id, values, url = "https://my.mci.ir/login", options = {}) => listeners.headers({
  tabId: id, url, type: "xmlhttprequest", statusCode: 200,
  responseHeaders: values.map(value => ({ name: "Set-Cookie", value })), ...options
});
const beginRestore = (id, url, requestId, requestHeaders = []) => {
  listeners.before({ tabId: id, type: "sub_frame", url, requestId });
  listeners.requestHeaders({ tabId: id, type: "sub_frame", url, requestId, requestHeaders });
};
const restoreHeaders = (id, url, requestId, values = [], options = {}) =>
  headers(id, values, url, { type: "sub_frame", requestId, ...options });

(async () => {
  await send(1, { type: "SET_POLICY", enabled: true, days: 10 });
  await send(2, { type: "SET_POLICY", enabled: true, days: 15 });
  await send(3, { type: "SET_POLICY", enabled: false });
  const first = headers(1, ["session=one; Path=/; Secure; HttpOnly; SameSite=Lax"]);
  assert.match(first.responseHeaders[0].value, /Max-Age=864000/);
  headers(2, ["session=two; Path=/; Secure; HttpOnly"]);
  assert.equal(headers(3, ["session=off; Path=/"]).responseHeaders, undefined);
  // Policy is bound to the page origin, not every network request made by that tab.
  const thirdParty = headers(1, ["other=private; Path=/"], "https://third-party.test/tracker");
  assert.equal(thirdParty.responseHeaders, undefined);
  const navigated = headers(1, ["other=private; Path=/"], "https://other-site.test/home");
  assert.equal(navigated.responseHeaders, undefined);
  const one = (await send(1, { type: "GET_CAPTURED_COOKIES" })).cookies;
  const two = (await send(2, { type: "GET_CAPTURED_COOKIES" })).cookies;
  assert.equal(one.length, 1);
  assert.equal(one[0].value, "one");
  assert.equal(one[0].isHttpOnly, true);
  assert.equal(one[0].isHostOnly, true);
  assert.equal(two[0].value, "two");
  listeners.before({ tabId: 1, type: "main_frame", url: "https://other-site.test/home" });
  assert.equal(headers(1, ["old=unmodified; Path=/"]).responseHeaders, undefined);
  assert.equal((await send(1, { type: "GET_CAPTURED_COOKIES" })).cookies.length, 0);
  const newOrigin = { tab: { id: 1 }, frameId: 0, url: "https://other-site.test/home" };
  await listeners.message({ type: "SET_POLICY", enabled: true, days: 8 }, newOrigin);
  assert.equal(headers(1, ["old=unmodified; Path=/"]).responseHeaders, undefined);
  await send(1, { type: "SET_POLICY", enabled: true, days: 10 });
  const explicit = headers(1, ["logout=; Max-Age=0; Path=/"]);
  assert.doesNotMatch(explicit.responseHeaders[0].value, /Max-Age=864000/);
  const capped = headers(1, ["short=ok; Max-Age=5; Path=/"]);
  assert.doesNotMatch(capped.responseHeaders[0].value, /Max-Age=864000/);
  headers(1, ["session=; Max-Age=0; Path=/"]);
  assert.equal((await send(1, { type: "GET_CAPTURED_COOKIES" })).cookies.some(c => c.name === "session"), false);

  const prepared = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  assert.ok(prepared.nonce);
  // The same nonce in another tab or origin must never receive these cookies.
  const restoreUrl = `https://my.mci.ir/?__marble_vault_restore=${prepared.nonce}`;
  listeners.before({ tabId: -1, type: "xmlhttprequest", url: restoreUrl });
  assert.equal(headers(-1, [], restoreUrl).responseHeaders?.length || 0, 0);
  listeners.before({ tabId: 2, type: "sub_frame", url: restoreUrl });
  assert.equal(headers(2, [], restoreUrl).responseHeaders.length, 0);
  assert.equal(headers(1, [], `https://evil.test/?__marble_vault_restore=${prepared.nonce}`).responseHeaders?.length || 0, 0);
  // A response with no matching request and cookie-header check cannot apply anything.
  assert.equal(restoreHeaders(1, restoreUrl, "unseen").responseHeaders.length, 0);
  beginRestore(1, restoreUrl, "request-1");
  assert.equal(restoreHeaders(1, restoreUrl, "wrong-request").responseHeaders.length, 0);
  const restored = restoreHeaders(1, restoreUrl, "request-1");
  assert.match(restored.responseHeaders[0].value, /^session=one;.*HttpOnly/);
  assert.doesNotMatch(restored.responseHeaders[0].value, /Domain=/);
  assert.equal(restoreHeaders(1, restoreUrl, "request-1").responseHeaders.length, 0, "restore is one-shot");
  const restoreStatus = await send(1, { type: "RESTORE_STATUS", nonce: prepared.nonce });
  assert.equal(restoreStatus.count, 1);
  assert.equal(restoreStatus.requestTab, "EXPECTED_TAB");
  assert.equal((await send(1, { type: "PREPARE_RESTORE", cookies: [{ ...one[0], domain: "evil.test" }] })).nonce, null);
  assert.equal((await send(1, { type: "PREPARE_RESTORE", cookies: [{ ...one[0], value: "x\r\nSet-Cookie: bad" }] })).nonce, null);
  const live = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  const liveUrl = `https://my.mci.ir/?__marble_vault_restore=${live.nonce}`;
  beginRestore(1, liveUrl, "live", [{ name: "Cookie", value: "session=newer" }]);
  assert.equal(restoreHeaders(1, liveUrl, "live").responseHeaders.length, 0, "preserve cookies already sent by Gecko");
  const rotated = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  const rotatedUrl = `https://my.mci.ir/?__marble_vault_restore=${rotated.nonce}`;
  beginRestore(1, rotatedUrl, "rotated");
  const serverHeaders = restoreHeaders(1, rotatedUrl, "rotated", ["session=newer; Path=/; HttpOnly"]).responseHeaders;
  assert.deepEqual(serverHeaders.map(h => h.value), ["session=newer; Path=/; HttpOnly"]);

  const cleared = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  const clearedUrl = `https://my.mci.ir/?__marble_vault_restore=${cleared.nonce}`;
  beginRestore(1, clearedUrl, "cleared");
  const clearHeaders = listeners.headers({
    tabId: 1, type: "sub_frame", requestId: "cleared", statusCode: 200, url: clearedUrl,
    responseHeaders: [{ name: "Clear-Site-Data", value: '"cookies", "storage"' }]
  }).responseHeaders;
  assert.equal(clearHeaders.length, 1, "server clear-site-data must not be undone");

  const unauthorized = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  const unauthorizedUrl = `https://my.mci.ir/?__marble_vault_restore=${unauthorized.nonce}`;
  beginRestore(1, unauthorizedUrl, "unauthorized");
  assert.equal(restoreHeaders(1, unauthorizedUrl, "unauthorized", [], { statusCode: 401 }).responseHeaders.length, 0);

  const navigating = await send(1, { type: "PREPARE_RESTORE", cookies: one });
  const navigatingUrl = `https://my.mci.ir/?__marble_vault_restore=${navigating.nonce}`;
  beginRestore(1, navigatingUrl, "navigating");
  listeners.before({ tabId: 1, type: "main_frame", url: "https://my.mci.ir/other" });
  assert.equal(restoreHeaders(1, navigatingUrl, "navigating").responseHeaders.length, 0, "same-origin navigation cancels restore");

  const spa = await send(1, { type: "PREPARE_RESTORE", restoreId: "old", cookies: one });
  const spaUrl = `https://my.mci.ir/?__marble_vault_restore=${spa.nonce}`;
  beginRestore(1, spaUrl, "spa");
  await send(1, { type: "CANCEL_RESTORE", restoreId: "old" });
  assert.equal(restoreHeaders(1, spaUrl, "spa").responseHeaders.length, 0, "native cancellation covers SPA navigation");

  const current = await send(1, { type: "PREPARE_RESTORE", restoreId: "current", cookies: one });
  const currentUrl = `https://my.mci.ir/?__marble_vault_restore=${current.nonce}`;
  await send(1, { type: "CANCEL_RESTORE", restoreId: "old" });
  beginRestore(1, currentUrl, "current");
  assert.equal(restoreHeaders(1, currentUrl, "current").responseHeaders.length, 1,
    "late cancellation must not remove a newer restore");
  listeners.removed(2);
  assert.equal((await send(2, { type: "GET_CAPTURED_COOKIES" })).cookies.length, 0);
  console.log("Session extension isolation, expiry and restore tests passed.");
})().catch(error => { console.error(error); process.exitCode = 1; });
