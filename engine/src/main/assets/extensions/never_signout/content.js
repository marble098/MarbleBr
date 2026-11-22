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

  // Each slow source gets a small budget: one lagging source (e.g. a busy page's
  // indexedDB.databases()) must not eat the whole snapshot timeout.
  function withBudget(fn, ms) {
    return new Promise(resolve => {
      let settled = false;
      const settle = value => { if (settled) return; settled = true; clearTimeout(timer); resolve(value); };
      const timer = setTimeout(() => settle(undefined), ms);
      try {
        Promise.resolve(fn()).then(settle, () => settle(undefined));
      } catch (_) { settle(undefined); }
    });
  }

  // Fallback when the background jar has nothing (site not enabled in Never sign out, or
  // Set-Cookie was ignored): every non-HttpOnly cookie visible through document.cookie.
  function parseDocumentCookies() {
    try {
      let host = location.hostname;
      if (!host) {
        try { host = new URL(location.origin || location.href).hostname; } catch (_) { host = ""; }
      }
      if (!host) return [];
      return String(document.cookie || "")
        .split(";")
        .map(pair => pair.trim())
        .filter(Boolean)
        .map(pair => {
          const i = pair.indexOf("=");
          if (i <= 0) return null;
          const name = pair.slice(0, i).trim();
          const value = pair.slice(i + 1).trim();
          if (!name) return null;
          return {
            name, value, domain: host, path: "/", isHostOnly: true,
            isSecure: location.protocol === "https:", isHttpOnly: false, sameSite: "unspecified",
            isSessionOnly: true, expiresEpochMillis: null
          };
        })
        .filter(Boolean);
    } catch (_) {
      return [];
    }
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
    const idbDbs = await withBudget(async () => {
      if (!window.indexedDB || typeof window.indexedDB.databases !== "function") return [];
      return (await window.indexedDB.databases()) || [];
    }, 1200);
    for (const db of idbDbs || []) if (db && db.name) idbDump[db.name] = String(db.version || 1);
    let cookies = [];
    const jarReply = await withBudget(() => browser.runtime.sendMessage({ type: "GET_CAPTURED_COOKIES" }), 1500);
    if (Array.isArray(jarReply?.cookies)) cookies = jarReply.cookies;
    if (!cookies.length) cookies = parseDocumentCookies();
    let persisted = false;
    try { persisted = (await withBudget(() => navigator.storage.persisted(), 400)) === true; } catch (_) {}
    return {
      event: "STORAGE_SNAPSHOT", origin: location.origin, href: location.href,
      cookies,
      localStorage: localDump, sessionStorage: sessionDump, indexedDbDump: idbDump,
      storagePersisted: persisted === true
    };
  }

  // A content-script fetch can be attributed to the extension rather than this tab in
  // GeckoView. Load a sandboxed, same-origin frame instead so webRequest sees the tab
  // and Set-Cookie is applied to that tab's actual session context. Never run page code.
  function requestRestoreInTab(url) {
    return new Promise(resolve => {
      const frame = document.createElement("iframe");
      frame.setAttribute("sandbox", "");
      frame.setAttribute("aria-hidden", "true");
      frame.hidden = true;
      frame.tabIndex = -1;
      frame.referrerPolicy = "no-referrer";
      let finished = false;
      const finish = loaded => {
        if (finished) return;
        finished = true;
        clearTimeout(timeout);
        frame.remove();
        resolve(loaded);
      };
      const timeout = setTimeout(() => finish(false), 4000);
      frame.onload = () => finish(true);
      frame.onerror = () => finish(false);
      const root = document.documentElement;
      if (!root) return finish(false);
      frame.src = url;
      root.appendChild(frame);
    });
  }

  async function restoreSnapshot(payload) {
    if (!payload || payload.origin !== location.origin) {
      return { cookieCount: 0, storageKeyCount: 0, cookieStatus: "ORIGIN_MISMATCH" };
    }
    let storageKeyCount = 0;
    for (const [storage, values] of [[window.localStorage, payload.localStorage], [window.sessionStorage, payload.sessionStorage]]) {
      if (!values || typeof values !== "object") continue;
      for (const [key, value] of Object.entries(values)) {
        try { storage.setItem(key, String(value)); storageKeyCount++; } catch (_) {}
      }
    }
    let cookieCount = 0;
    let cookieStatus = "NO_COOKIES";
    if (!Array.isArray(payload.cookies)) {
      cookieStatus = "INVALID_COOKIES";
    } else if (payload.cookies.length) {
      try {
        const prepared = await browser.runtime.sendMessage({ type: "PREPARE_RESTORE", cookies: payload.cookies });
        cookieStatus = "NO_ELIGIBLE_COOKIES";
        if (prepared?.nonce) {
          const restoreUrl = new URL("/", location.origin);
          restoreUrl.searchParams.set("__marble_vault_restore", prepared.nonce);
          const requestSucceeded = await requestRestoreInTab(restoreUrl.href);
          const status = await browser.runtime.sendMessage({ type: "RESTORE_STATUS", nonce: prepared.nonce });
          cookieCount = Number(status?.count) || 0;
          if (cookieCount > 0) cookieStatus = "HEADERS_PROPOSED";
          else if (status?.requestTab === "NO_TAB") cookieStatus = "REQUEST_NO_TAB";
          else if (status?.requestTab === "OTHER_TAB") cookieStatus = "REQUEST_OTHER_TAB";
          else if (status?.requestTab === "NOT_SEEN") cookieStatus = "REQUEST_NOT_SEEN";
          else cookieStatus = requestSucceeded ? "RESPONSE_NOT_MATCHED" : "FRAME_FAILED";
        }
      } catch (_) { cookieStatus = "BRIDGE_FAILED"; }
    }
    return { cookieCount, storageKeyCount, cookieStatus };
  }

  function sendSnapshot() {
    if (!port || !autoCaptureEnabled) return;
    collectStorageSnapshot().then(snapshot => port.postMessage(snapshot)).catch(() => {});
  }

  // ---------------------------------------------------------------------------
  // «ذخیرهٔ هوشمندِ ورود»: لحظهٔ ارسالِ فرمِ ورود را می‌گیریم و همان‌جا (پیش از
  // ناوبری) مقادیر را یک بار می‌خوانیم. تصمیمِ «ذخیره کنم؟» و رمزنگاری در خودِ
  // اپ گرفته می‌شود؛ اسکریپت هیچ‌چیز را ذخیره یا ارسال نمی‌کند.
  // ---------------------------------------------------------------------------
  let lastSubmitAt = 0;
  const SUBMIT_DEDUPE_MS = 1500;

  function readableLoginValues() {
    try {
      if (typeof document.querySelectorAll !== "function") return null;
      const visible = el => !!(el.getClientRects && el.getClientRects().length) || !!el.offsetParent;
      const passwords = [].slice.call(document.querySelectorAll("input[type=password]")).filter(visible);
      const otp = [].slice.call(document.querySelectorAll(
        "input[autocomplete=one-time-code],input[inputmode=numeric],input[maxlength='1'],input[maxlength='2']"
      )).filter(visible);
      const otpHint = [].slice.call(document.querySelectorAll("input")).filter(el => {
        if (!visible(el)) return false;
        const type = (el.getAttribute("type") || "text").toLowerCase();
        if (type === "password" || type === "hidden" || type === "email") return false;
        const hay = ((el.getAttribute("name") || "") + " " + (el.getAttribute("id") || "") + " " +
          (el.getAttribute("placeholder") || "") + " " + (el.getAttribute("autocomplete") || "")).toLowerCase();
        return /otp|one[-_ ]?time|verif|sms|token|passcode|auth|pin|2fa|mfa|code|کد|رمز|تایید|تأیید/.test(hay);
      });
      const otpCount = Math.max(otp.length, otpHint.length);
      // خطاهای رایجِ ورود، مستقل از زبان: عناصری با نقش هشدار/متنِ قرمز که پیام کوتاه دارند.
      let errorVisible = false;
      try {
        const alerts = [].slice.call(document.querySelectorAll("[role=alert],[aria-live=assertive],.error,.error-message,.alert,.invalid-feedback"));
        errorVisible = alerts.some(el => visible(el) && ((el.innerText || "").trim().length >= 4));
        if (!errorVisible) {
          const lowered = (document.body ? (document.body.innerText || "") : "").toLowerCase();
          errorVisible = /(رمز|گذرواژه|password|credentials?).{0,24}(اشتباه|نادرست|نامعتبر|incorrect|invalid|wrong|mismatch)/i.test(lowered);
        }
      } catch (_) {}
      if (!passwords.length) {
        // فرمِ بدونِ گذرواژه (مثلاً فقط کدِ پیامکی): فقط برای تصمیمِ «کد را بگذار» ارزش دارد.
        if (otpCount > 0) {
          return { username: "", password: "", otpFields: otpCount, otpOnly: true, errorVisible: errorVisible };
        }
        return null;
      }
      const password = passwords[0];
      if (!password.value) return null;
      const form = (password.closest && password.closest("form")) || document;
      let users = [].slice.call(form.querySelectorAll(
        "input[type=text],input[type=email],input[type=tel],input[autocomplete=username],input[name=username],input[name=email]"
      )).filter(visible);
      if (!users.length) {
        users = [].slice.call(document.querySelectorAll(
          "input[type=text],input[type=email],input[type=tel],input[autocomplete=username]"
        )).filter(visible);
      }
      const usernameValue = users.length ? (users[0].value || "") : "";
      return {
        username: String(usernameValue).slice(0, 320),
        password: password.value,
        otpFields: otpCount,
        otpOnly: false,
        errorVisible: errorVisible
      };
    } catch (_) { return null; }
  }

  function postValues(values) {
    return {
      event: "LOGIN_SUBMIT",
      url: location.href,
      username: values.username,
      password: values.password,
      otpFields: values.otpFields,
      errorVisible: values.errorVisible === true
    };
  }

  function reportLoginSubmit() {
    if (!port) return;
    const now = Date.now();
    if (now - lastSubmitAt < SUBMIT_DEDUPE_MS) return;
    const values = readableLoginValues();
    if (!values) return;
    lastSubmitAt = now;
    try { port.postMessage(postValues(values)); } catch (_) {}
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
        let result = { cookieCount: 0, storageKeyCount: 0, cookieStatus: "BRIDGE_FAILED" };
        try { result = await restoreSnapshot(message.payload); } catch (_) {}
        port.postMessage({ event: "RESTORE_RESULT", requestId: message.requestId, ...result });
      } else if (message.type === "EVAL_JS") {
        let result = "";
        try { result = String(eval(message.script) ?? ""); } catch (_) {}
        port.postMessage({ event: "JS_RESULT", requestId: message.requestId, result });
      } else if (message.type === "CAPTURE_PAGE") {
        // The caller waits on this response, so always answer: even a failed collection is
        // answered with a (possibly empty) snapshot instead of leaving the caller hanging.
        let snapshot = null;
        try { snapshot = await collectStorageSnapshot(); } catch (_) { snapshot = null; }
        const finalSnapshot = snapshot || {
          event: "STORAGE_SNAPSHOT", origin: location.origin, href: location.href,
          cookies: [], localStorage: {}, sessionStorage: {}, indexedDbDump: {}, storagePersisted: false
        };
        try { port.postMessage({ ...finalSnapshot, requestId: message.requestId }); } catch (_) {}
      }
    });
  } catch (_) {}

  // ارسالِ فرم با Enter، با دکمهٔ submit یا با کلیک روی دکمهٔ ورودِ اسکریپتی.
  document.addEventListener("submit", reportLoginSubmit, true);
  document.addEventListener("click", event => {
    try {
      const target = event.target;
      if (!target || typeof target.closest !== "function") return;
      const button = target.closest("button,input[type=submit],[role=button]");
      if (!button) return;
      const label = ((button.innerText || button.value || button.getAttribute("aria-label") || "") + "").toLowerCase();
      if (/log ?in|sign ?in|ورود|submit|continue|ادامه/.test(label)) reportLoginSubmit();
    } catch (_) {}
  }, true);

  // ---------------------------------------------------------------------------
  // «رمزِ یکبارمصرف»: به‌محضِ دیده‌شدنِ کادرِ کد، به اپ خبر می‌دهیم تا کد را از پیامک/اعلان
  // پیدا کند و بنویسد. هیچ کدی این‌جا خوانده یا منتقل نمی‌شود؛ فقط یک اعلانِ «کادر آماده است».
  // ---------------------------------------------------------------------------
  let lastOtpNoticeAt = 0;
  const OTP_NOTICE_DEDUPE_MS = 2000;

  function looksLikeCodeScreen() {
    try {
      if (typeof document.querySelectorAll !== "function") return false;
      if (location.pathname && /verify|verification|otp|2fa|mfa|code|confirm|تایید|تأیید/.test(location.href) === false) {
        // ادامه می‌دهیم؛ خودِ کادرها معیارِ اصلی‌اند.
      }
      const inputs = [].slice.call(document.querySelectorAll("input")).filter(el => {
        if (!el.getClientRects || !(el.getClientRects().length || el.offsetParent)) return false;
        const type = (el.getAttribute("type") || "text").toLowerCase();
        return type !== "password" && type !== "hidden" && type !== "email" && type !== "file";
      });
      if (!inputs.length) return false;
      const passwordVisible = [].slice.call(document.querySelectorAll("input[type=password]"))
        .some(el => !!(el.getClientRects && el.getClientRects().length) || !!el.offsetParent);
      if (passwordVisible) return false;
      return inputs.some(el => {
        const max = parseInt(el.getAttribute("maxlength") || "0", 10) || 0;
        const ac = (el.getAttribute("autocomplete") || "").toLowerCase();
        const hay = ((el.getAttribute("name") || "") + " " + (el.getAttribute("id") || "") + " " +
          (el.getAttribute("placeholder") || "") + " " + ac + " " + (el.getAttribute("inputmode") || "")).toLowerCase();
        return ac === "one-time-code" || max === 1 || max === 2 ||
          /otp|one[-_ ]?time|verif|sms|token|passcode|auth|pin|2fa|mfa|code|کد|رمز|تایید|تأیید|پویا|پیامک/.test(hay);
      });
    } catch (_) { return false; }
  }

  function announceCodeScreen() {
    if (!port) return;
    const now = Date.now();
    if (now - lastOtpNoticeAt < OTP_NOTICE_DEDUPE_MS) return;
    if (!looksLikeCodeScreen()) return;
    lastOtpNoticeAt = now;
    try { port.postMessage({ event: "OTP_SCREEN_READY", url: location.href }); } catch (_) {}
  }

  window.addEventListener("DOMContentLoaded", () => setTimeout(sendSnapshot, 1200));
  window.addEventListener("DOMContentLoaded", () => setTimeout(announceCodeScreen, 1500));
  window.addEventListener("load", () => setTimeout(announceCodeScreen, 800));
  // صفحه‌های SPA کادرِ کد را بعد از کلیک می‌سازند؛ تغییراتِ DOM را سبک دنبال می‌کنیم.
  try {
    let pending = null;
    const observer = new MutationObserver(() => {
      if (pending) return;
      pending = setTimeout(() => { pending = null; announceCodeScreen(); }, 1200);
    });
    if (document.documentElement) {
      observer.observe(document.documentElement, { childList: true, subtree: true });
    }
  } catch (_) {}
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
