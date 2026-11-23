/* Run the actual content script in a minimal WebExtension/DOM harness. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const listeners = {};
const sent = [];
const runtimeMessages = [];
let persistCalls = 0;
let prepared = { nonce: null };
let restoredCount = 0;
let frameFails = false;
let lastFrame = null;
let captureGate = null;
let prepareGate = null;
const event = name => ({ addListener: fn => { listeners[name] = fn; } });
const makeStorage = initial => {
  const items = new Map(Object.entries(initial));
  return {
    get length() { return items.size; },
    key: index => [...items.keys()][index],
    getItem: key => items.get(key) ?? null,
    setItem: (key, value) => items.set(key, value)
  };
};
const localStorage = makeStorage({ user: "one" });
const sessionStorage = makeStorage({ session: "two" });
const port = { onMessage: event("port"), postMessage: msg => sent.push(msg) };
const browser = { runtime: {
  connectNative: () => port,
  sendMessage: async msg => {
    runtimeMessages.push(msg);
    if (msg.type === "GET_CAPTURED_COOKIES") return captureGate ? await captureGate : { cookies: [] };
    if (msg.type === "PREPARE_RESTORE") return prepareGate ? await prepareGate : prepared;
    if (msg.type === "RESTORE_STATUS") return { count: restoredCount };
    return { ok: true };
  },
  onMessage: event("runtime")
} };
const window = {
  localStorage, sessionStorage,
  indexedDB: { databases: async () => [] },
  addEventListener: (name, fn) => { listeners[name] = fn; }
};
const document = {
  visibilityState: "visible",
  addEventListener: (name, fn) => { listeners[name] = fn; },
  createElement: name => {
    assert.equal(name, "iframe");
    lastFrame = { attributes: {}, setAttribute(key, value) { this.attributes[key] = value; }, remove() { this.removed = true; } };
    return lastFrame;
  },
  documentElement: { appendChild: frame => { if (frameFails) frame.onerror(); else frame.onload(); } }
};
const navigator = { storage: { persisted: async () => false, persist: async () => { persistCalls++; return true; } } };
const timers = [];
const location = { origin: "https://my.mci.ir", href: "https://my.mci.ir/panel" };
const code = fs.readFileSync("engine/src/main/assets/extensions/never_signout/content.js", "utf8");
vm.runInNewContext(code, {
  browser, window, document, navigator, location,
  URL, AbortController, setInterval: fn => { listeners.interval = fn; }, setTimeout: fn => { timers.push(fn); return 1; },
  clearTimeout: () => {}
});

(async () => {
  listeners.DOMContentLoaded();
  timers.shift()();
  listeners.visibilitychange();
  await Promise.resolve();
  assert.equal(persistCalls, 0, "unprotected pages must not request persistent storage");
  assert.equal(sent.length, 0, "unprotected pages must not send automatic snapshots");

  document.cookie = "unknown_scope=private";
  await listeners.port({ type: "CAPTURE_PAGE", requestId: "manual" });
  assert.equal(sent[0].requestId, "manual", "explicit manual capture still works when protection is off");
  assert.equal(sent[0].cookies.length, 0, "document.cookie lacks scope metadata and must not be guessed");
  assert.equal(sent[0].localStorage.user, "one");
  assert.equal(sent[0].sessionStorage.session, "two");

  let releaseCapture;
  captureGate = new Promise(resolve => { releaseCapture = resolve; });
  const oldUrl = location.href;
  const staleCapture = listeners.port({ type: "CAPTURE_PAGE", requestId: "stale", expectedUrl: oldUrl });
  await new Promise(resolve => setImmediate(resolve));
  location.href = "https://my.mci.ir/next-route";
  releaseCapture({ cookies: [] });
  await staleCapture;
  captureGate = null;
  assert.equal(sent.at(-1).event, "CAPTURE_FAILED", "route changes invalidate an in-flight capture");
  location.href = oldUrl;
  captureGate = new Promise(resolve => { releaseCapture = resolve; });
  const currentCapture = listeners.port({ type: "CAPTURE_PAGE", requestId: "current", expectedUrl: oldUrl });
  await new Promise(resolve => setImmediate(resolve));
  localStorage.setItem("user", "after-logout");
  releaseCapture({ cookies: [] });
  await currentCapture;
  captureGate = null;
  assert.equal(sent.at(-1).localStorage.user, "after-logout", "slow sources must not freeze stale storage");
  localStorage.setItem("user", "one");

  await listeners.port({ type: "SET_COOKIE_POLICY", enabled: true, days: 30, persistStorage: true });
  await Promise.resolve();
  await Promise.resolve();
  assert.equal(persistCalls, 1);
  assert.equal(runtimeMessages.at(-1).enabled, true);
  document.visibilityState = "hidden";
  listeners.visibilitychange();
  await new Promise(resolve => setImmediate(resolve));
  assert.ok(sent.some(message => message.event === "AUTO_CAPTURE_REQUEST" && message.url === location.href));
  assert.equal(sent.some(message => message.event === "STORAGE_SNAPSHOT" && !message.requestId), false,
    "automatic captures must be bound by the native caller");

  document.visibilityState = "visible";
  const evaluate = (expectedUrl, requireVisible = false) => listeners.port({
    type: "EVAL_JS", requestId: "fill", expectedUrl, requireVisible, script: "'filled'"
  });
  await evaluate(location.href);
  assert.equal(sent.at(-1).result, "filled");
  await evaluate("https://my.mci.ir/old-route");
  assert.equal(sent.at(-1).result, "");
  await evaluate("https://my.mci.ir:8443/panel");
  assert.equal(sent.at(-1).result, "");
  document.visibilityState = "hidden";
  await evaluate(location.href, true);
  assert.equal(sent.at(-1).result, "", "login and OTP scripts must not run in a hidden tab");
  document.visibilityState = "visible";
  await evaluate(location.href, true);
  assert.equal(sent.at(-1).result, "filled");
  listeners.pagehide();
  await evaluate(location.href);
  assert.equal(sent.at(-1).result, "", "same-URL reload must not fill the document being left");
  listeners.pageshow();

  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "wrong-origin", payload: {
    origin: "https://other.test", localStorage: { injected: "no" }
  } });
  assert.equal(localStorage.getItem("injected"), null);
  assert.equal(sent.at(-1).storageKeyCount, 0);
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "different-route", expectedUrl: "https://my.mci.ir/old", payload: {
    origin: location.origin, localStorage: { injected: "no" }
  } });
  assert.equal(localStorage.getItem("injected"), null);
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "same-origin", payload: {
    origin: "https://my.mci.ir", localStorage: { restored: "yes", user: "old value" }
  } });
  assert.equal(localStorage.getItem("restored"), "yes");
  assert.equal(localStorage.getItem("user"), "one", "restore preserves live storage");
  assert.equal(sent.at(-1).storageKeyCount, 1);

  const cookiePayload = { origin: "https://my.mci.ir", cookies: [{ name: "session", value: "private" }] };
  let releasePrepare;
  prepareGate = new Promise(resolve => { releasePrepare = resolve; });
  const staleRestore = listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "stale-restore", expectedUrl: location.href, payload: cookiePayload });
  await new Promise(resolve => setImmediate(resolve));
  await listeners.port({ type: "CANCEL_VAULT_RESTORE", requestId: "stale-restore" });
  let releaseCurrent;
  prepareGate = new Promise(resolve => { releaseCurrent = resolve; });
  const currentRestore = listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "current-restore", expectedUrl: location.href, payload: cookiePayload });
  await new Promise(resolve => setImmediate(resolve));
  releasePrepare({ nonce: "stale" });
  await staleRestore;
  assert.equal(lastFrame, null, "a cancelled restore must not launch a frame even if the URL matches again");
  assert.ok(runtimeMessages.some(message => message.type === "CANCEL_RESTORE" && message.restoreId === "stale-restore"));
  await listeners.port({ type: "CANCEL_VAULT_RESTORE", requestId: "stale-restore" });
  assert.equal(runtimeMessages.some(message => message.type === "CANCEL_RESTORE" && message.restoreId === "current-restore"), false,
    "a late native cancellation must not cancel a newer restore");
  releaseCurrent({ nonce: "current" });
  await currentRestore;
  assert.equal(lastFrame.src, "https://my.mci.ir/?__marble_vault_restore=current");
  prepareGate = null;

  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "no-eligible", payload: cookiePayload });
  assert.equal(sent.at(-1).cookieStatus, "NO_ELIGIBLE_COOKIES");
  prepared = { nonce: "nonce" };
  frameFails = true;
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "frame-failure", payload: cookiePayload });
  assert.equal(sent.at(-1).cookieStatus, "FRAME_FAILED");
  frameFails = false;
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "unmatched", payload: cookiePayload });
  assert.equal(sent.at(-1).cookieStatus, "RESPONSE_NOT_MATCHED");
  assert.equal(lastFrame.attributes.sandbox, "");
  assert.equal(lastFrame.hidden, true);
  assert.equal(lastFrame.referrerPolicy, "no-referrer");
  assert.equal(lastFrame.src, "https://my.mci.ir/?__marble_vault_restore=nonce");
  assert.equal(lastFrame.removed, true);
  restoredCount = 1;
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "proposed", payload: cookiePayload });
  assert.equal(sent.at(-1).cookieStatus, "HEADERS_PROPOSED");
  assert.equal(sent.at(-1).cookieCount, 1);

  await listeners.port({ type: "SET_COOKIE_POLICY", enabled: false, persistStorage: true });
  const count = sent.length;
  listeners.visibilitychange();
  await Promise.resolve();
  assert.equal(sent.length, count, "disabling protection stops automatic snapshots immediately");
  assert.equal(persistCalls, 1);
  console.log("Session content policy, manual capture and origin tests passed.");
})().catch(error => { console.error(error); process.exitCode = 1; });
