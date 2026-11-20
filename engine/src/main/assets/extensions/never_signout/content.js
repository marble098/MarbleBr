/* Content script runs only in a normal top-level page, never in private browsing. */
(function () {
  "use strict";
  const NATIVE_APP = "marble_never_signout";
  let port = null;
  let autoCaptureEnabled = false;

  async function ensurePersistentStorage() {
    try {
      if (navigator.storage && typeof navigator.storage.persist === "function" && !(await navigator.storage.persisted())) {
        await navigator.storage.persist();
      }
    } catch (_) {}
  }

  async function collectStorageSnapshot() {
    const localDump = {};
    const sessionDump = {};
    const idbDump = {};
    try {
      for (let i = 0; i < window.localStorage.length; i++) {
        const k = window.localStorage.key(i);
        if (k) localDump[k] = window.localStorage.getItem(k) || "";
      }
    } catch (_) {}
    try {
      for (let i = 0; i < window.sessionStorage.length; i++) {
        const k = window.sessionStorage.key(i);
        if (k) sessionDump[k] = window.sessionStorage.getItem(k) || "";
      }
    } catch (_) {}
    // Only names and versions are available here; this is NOT an IndexedDB backup.
    try {
      if (window.indexedDB && typeof window.indexedDB.databases === "function") {
        const dbs = await window.indexedDB.databases();
        for (const db of dbs) if (db && db.name) idbDump[db.name] = String(db.version || 1);
      }
    } catch (_) {}
    let cookies = [];
    try {
      const reply = await browser.runtime.sendMessage({ type: "GET_CAPTURED_COOKIES" });
      if (Array.isArray(reply?.cookies)) cookies = reply.cookies;
    } catch (_) {}
    let persisted = false;
    try { persisted = await navigator.storage.persisted(); } catch (_) {}
    return {
      event: "STORAGE_SNAPSHOT", origin: location.origin, href: location.href,
      cookies,
      localStorage: localDump, sessionStorage: sessionDump, indexedDbDump: idbDump,
      storagePersisted: persisted === true
    };
  }

  async function restoreSnapshot(payload) {
    if (!payload || payload.origin !== location.origin) return { cookieCount: 0, storageKeyCount: 0 };
    let storageKeyCount = 0;
    for (const [storage, values] of [[window.localStorage, payload.localStorage], [window.sessionStorage, payload.sessionStorage]]) {
      if (!values || typeof values !== "object") continue;
      for (const [key, value] of Object.entries(values)) {
        try { storage.setItem(key, String(value)); storageKeyCount++; } catch (_) {}
      }
    }
    let cookieCount = 0;
    if (Array.isArray(payload.cookies) && payload.cookies.length) {
      try {
        const prepared = await browser.runtime.sendMessage({ type: "PREPARE_RESTORE", cookies: payload.cookies });
        if (prepared?.nonce) {
          const restoreUrl = new URL("/", location.origin);
          restoreUrl.searchParams.set("__marble_vault_restore", prepared.nonce);
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), 4000);
          try {
            await fetch(restoreUrl.href, { credentials: "include", cache: "no-store", redirect: "manual", signal: controller.signal });
          } catch (_) {} finally { clearTimeout(timeout); }
          const status = await browser.runtime.sendMessage({ type: "RESTORE_STATUS", nonce: prepared.nonce });
          cookieCount = Number(status?.count) || 0;
        }
      } catch (_) {}
    }
    return { cookieCount, storageKeyCount };
  }

  function sendSnapshot() {
    if (!port || !autoCaptureEnabled) return;
    collectStorageSnapshot().then(snapshot => port.postMessage(snapshot)).catch(() => {});
  }

  try {
    port = browser.runtime.connectNative(NATIVE_APP);
    port.onMessage.addListener(async message => {
      if (!message || !message.type) return;
      if (message.type === "SET_COOKIE_POLICY") {
        autoCaptureEnabled = message.enabled === true;
        try { await browser.runtime.sendMessage({ type: "SET_POLICY", enabled: autoCaptureEnabled, days: message.days }); } catch (_) {}
        if (autoCaptureEnabled && message.persistStorage === true) ensurePersistentStorage();
      } else if (message.type === "APPLY_VAULT_SNAPSHOT") {
        let result = { cookieCount: 0, storageKeyCount: 0 };
        try { result = await restoreSnapshot(message.payload); } catch (_) {}
        port.postMessage({ event: "RESTORE_RESULT", requestId: message.requestId, ...result });
      } else if (message.type === "EVAL_JS") {
        let result = "";
        try { result = String(eval(message.script) ?? ""); } catch (_) {}
        port.postMessage({ event: "JS_RESULT", requestId: message.requestId, result });
      } else if (message.type === "CAPTURE_PAGE") {
        try { port.postMessage({ ...(await collectStorageSnapshot()), requestId: message.requestId }); } catch (_) {}
      }
    });
  } catch (_) {}

  window.addEventListener("DOMContentLoaded", () => setTimeout(sendSnapshot, 1200));
  document.addEventListener("visibilitychange", () => { if (document.visibilityState === "hidden") sendSnapshot(); });
  // SPA sign-ins can happen well after DOMContentLoaded.
  setInterval(() => { if (document.visibilityState === "visible") sendSnapshot(); }, 60000);
  browser.runtime.onMessage.addListener(message => {
    if (message?.type === "HTTP_UNAUTHORIZED" && originOf(message.url) === location.origin && port) {
      port.postMessage({ event: "HTTP_UNAUTHORIZED", url: message.url, statusCode: message.statusCode });
    }
  });
  function originOf(url) {
    try { return new URL(url).origin; } catch (_) { return null; }
  }
})();
