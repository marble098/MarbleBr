/* MarbleBr Never Sign-out Engine - Content Script */
(function () {
  "use strict";

  const NATIVE_APP = "marble_never_signout";

  // 1. Request persistent storage automatically so Android OS never evicts site storage
  async function ensurePersistentStorage() {
    try {
      if (navigator.storage && typeof navigator.storage.persist === "function") {
        const alreadyPersisted = await navigator.storage.persisted();
        if (!alreadyPersisted) {
          await navigator.storage.persist();
        }
      }
    } catch (e) {
      // Ignore on restricted origins
    }
  }

  // 2. Collect localStorage, sessionStorage & IndexedDB summary for Session Vault snapshot
  async function collectStorageSnapshot() {
    const localDump = {};
    const sessionDump = {};
    const idbDump = {};

    try {
      for (let i = 0; i < window.localStorage.length; i++) {
        const k = window.localStorage.key(i);
        if (k) localDump[k] = window.localStorage.getItem(k) || "";
      }
    } catch (e) {}

    try {
      for (let i = 0; i < window.sessionStorage.length; i++) {
        const k = window.sessionStorage.key(i);
        if (k) sessionDump[k] = window.sessionStorage.getItem(k) || "";
      }
    } catch (e) {}

    try {
      if (window.indexedDB && typeof window.indexedDB.databases === "function") {
        const dbs = await window.indexedDB.databases();
        for (const dbInfo of dbs) {
          if (dbInfo && dbInfo.name) {
            idbDump[dbInfo.name] = String(dbInfo.version || 1);
          }
        }
      }
    } catch (e) {}

    return {
      event: "STORAGE_SNAPSHOT",
      origin: window.location.origin,
      href: window.location.href,
      cookieString: document.cookie || "",
      localStorage: localDump,
      sessionStorage: sessionDump,
      indexedDbDump: idbDump,
      storagePersisted: await navigator.storage?.persisted?.().catch(() => false) === true
    };
  }

  // 3. Apply restored storage snapshot from Session Vault / Time Machine
  function restoreStorageSnapshot(payload) {
    if (!payload) return;
    try {
      if (payload.localStorage) {
        for (const [k, v] of Object.entries(payload.localStorage)) {
          window.localStorage.setItem(k, String(v));
        }
      }
      if (payload.sessionStorage) {
        for (const [k, v] of Object.entries(payload.sessionStorage)) {
          window.sessionStorage.setItem(k, String(v));
        }
      }
      if (Array.isArray(payload.cookies)) {
        for (const c of payload.cookies) {
          if (!c.isHttpOnly && c.name) {
            const maxAge = 365 * 24 * 60 * 60;
            document.cookie = `${c.name}=${c.value}; path=${c.path || "/"}; max-age=${maxAge}; SameSite=Lax`;
          }
        }
      }
    } catch (e) {}
  }

  ensurePersistentStorage();

  try {
    const port = browser.runtime.connectNative(NATIVE_APP);
    port.onMessage.addListener(async (message) => {
      if (!message || !message.type) return;
      if (message.type === "APPLY_VAULT_SNAPSHOT") {
        // بازگردانیِ نسخهٔ «ماشین زمان»: نوشتنِ حافظهٔ محلی روی همین صفحه.
        // بازخوانی (reload) را سمت بومی انجام می‌دهد تا ترتیبِ نوشتنِ کوکی‌ها هم رعایت شود.
        restoreStorageSnapshot(message.payload);
        return;
      }
      if (message.type === "EVAL_JS") {
        // اجرای اسکریپتِ بومی (حالت مطالعه / استخراج متنِ بلندخوانی). GeckoView هیچ
        // evaluateJS رویِ GeckoSession ندارد؛ تنها مسیرِ پشتیبانی‌شده همین پورت است.
        let result = "";
        try {
          result = String(eval(message.script) ?? "");
        } catch (e) {
          result = "";
        }
        port.postMessage({ event: "JS_RESULT", requestId: message.requestId, result: result });
        return;
      }
      if (message.type !== "CAPTURE_PAGE") return;
      try {
        const snapshot = await collectStorageSnapshot();
        port.postMessage({ ...snapshot, requestId: message.requestId });
      } catch (e) {}
    });
  } catch (e) {}

  window.addEventListener("DOMContentLoaded", () => {
    setTimeout(async () => {
      try {
        const snapshot = await collectStorageSnapshot();
        browser.runtime.sendNativeMessage(NATIVE_APP, snapshot).catch(() => {});
      } catch (e) {}
    }, 1200);
  });

  browser.runtime.onMessage.addListener((msg) => {
    if (msg && msg.type === "APPLY_VAULT_SNAPSHOT") {
      restoreStorageSnapshot(msg.payload);
      window.location.reload();
    }
  });
})();
