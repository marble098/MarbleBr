/* Run the actual content script in a minimal WebExtension/DOM harness. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");

const listeners = {};
const sent = [];
const runtimeMessages = [];
let persistCalls = 0;
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
  sendMessage: async msg => { runtimeMessages.push(msg); return msg.type === "GET_CAPTURED_COOKIES" ? { cookies: [] } : { ok: true }; },
  onMessage: event("runtime")
} };
const window = {
  localStorage, sessionStorage,
  indexedDB: { databases: async () => [] },
  addEventListener: (name, fn) => { listeners[name] = fn; }
};
const document = { visibilityState: "visible", addEventListener: (name, fn) => { listeners[name] = fn; } };
const navigator = { storage: { persisted: async () => false, persist: async () => { persistCalls++; return true; } } };
const timers = [];
const code = fs.readFileSync("engine/src/main/assets/extensions/never_signout/content.js", "utf8");
vm.runInNewContext(code, {
  browser, window, document, navigator, location: { origin: "https://my.mci.ir", href: "https://my.mci.ir/panel" },
  URL, AbortController, setInterval: fn => { listeners.interval = fn; }, setTimeout: fn => { timers.push(fn); return 1; },
  clearTimeout: () => {}
});

(async () => {
  listeners.DOMContentLoaded();
  timers.shift()();
  listeners.interval();
  await Promise.resolve();
  assert.equal(persistCalls, 0, "unprotected pages must not request persistent storage");
  assert.equal(sent.length, 0, "unprotected pages must not send automatic snapshots");

  await listeners.port({ type: "CAPTURE_PAGE", requestId: "manual" });
  assert.equal(sent[0].requestId, "manual", "explicit manual capture still works when protection is off");
  assert.equal(sent[0].localStorage.user, "one");
  assert.equal(sent[0].sessionStorage.session, "two");

  await listeners.port({ type: "SET_COOKIE_POLICY", enabled: true, days: 30, persistStorage: true });
  await Promise.resolve();
  await Promise.resolve();
  assert.equal(persistCalls, 1);
  assert.equal(runtimeMessages.at(-1).enabled, true);
  listeners.interval();
  await new Promise(resolve => setImmediate(resolve));
  assert.ok(sent.some(message => message.event === "STORAGE_SNAPSHOT" && !message.requestId));

  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "wrong-origin", payload: {
    origin: "https://other.test", localStorage: { injected: "no" }
  } });
  assert.equal(localStorage.getItem("injected"), null);
  assert.equal(sent.at(-1).storageKeyCount, 0);
  await listeners.port({ type: "APPLY_VAULT_SNAPSHOT", requestId: "same-origin", payload: {
    origin: "https://my.mci.ir", localStorage: { restored: "yes" }
  } });
  assert.equal(localStorage.getItem("restored"), "yes");
  assert.equal(sent.at(-1).storageKeyCount, 1);

  await listeners.port({ type: "SET_COOKIE_POLICY", enabled: false, persistStorage: true });
  const count = sent.length;
  listeners.interval();
  await Promise.resolve();
  assert.equal(sent.length, count, "disabling protection stops automatic snapshots immediately");
  assert.equal(persistCalls, 1);
  console.log("Session content policy, manual capture and origin tests passed.");
})().catch(error => { console.error(error); process.exitCode = 1; });
