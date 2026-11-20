/* Per-tab cookie handling. Never use browser.cookies.set: GeckoView contextId is not a WebExtension storeId. */
(function () {
  "use strict";

  const MAX_COOKIES = 500;
  const MAX_DAYS = 730;
  const policies = new Map();
  const jars = new Map();
  const restores = new Map();

  function originOf(url) {
    try {
      const parsed = new URL(url);
      return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.origin : null;
    } catch (_) {
      return null;
    }
  }

  function defaultPath(path) {
    const lastSlash = path.lastIndexOf("/");
    return lastSlash <= 0 ? "/" : path.slice(0, lastSlash);
  }

  function parseCookie(header, url, now) {
    const parsed = new URL(url);
    const parts = header.split(";");
    const pair = parts.shift().trim();
    const separator = pair.indexOf("=");
    if (separator <= 0) return null;
    const name = pair.slice(0, separator).trim();
    const value = pair.slice(separator + 1).trim();
    if (!/^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/.test(name)) return null;
    const cookie = {
      name, value, domain: parsed.hostname.toLowerCase(), path: defaultPath(parsed.pathname),
      isHostOnly: true, isSecure: false, isHttpOnly: false, sameSite: "unspecified",
      isSessionOnly: true, expiresEpochMillis: null
    };
    let maxAge = null;
    for (const part of parts) {
      const index = part.indexOf("=");
      const key = (index < 0 ? part : part.slice(0, index)).trim().toLowerCase();
      const val = index < 0 ? "" : part.slice(index + 1).trim();
      if (key === "domain" && val) {
        const domain = val.replace(/^\./, "").toLowerCase();
        if (parsed.hostname.toLowerCase() !== domain && !parsed.hostname.toLowerCase().endsWith("." + domain)) return null;
        cookie.domain = domain;
        cookie.isHostOnly = false;
      } else if (key === "path" && val.startsWith("/")) cookie.path = val;
      else if (key === "secure") cookie.isSecure = true;
      else if (key === "httponly") cookie.isHttpOnly = true;
      else if (key === "samesite") cookie.sameSite = val.toLowerCase();
      else if (key === "max-age" && /^-?\d+$/.test(val)) maxAge = Number(val);
      else if (key === "expires") {
        const expiry = Date.parse(val);
        if (Number.isFinite(expiry)) cookie.expiresEpochMillis = expiry;
      }
    }
    if (maxAge !== null && Number.isFinite(maxAge)) cookie.expiresEpochMillis = now + maxAge * 1000;
    cookie.isSessionOnly = cookie.expiresEpochMillis === null;
    return cookie;
  }

  function cookieHeader(cookie, host, protocol, now) {
    if (!cookie || typeof cookie.name !== "string" || !/^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/.test(cookie.name)) return null;
    if (typeof cookie.value !== "string" || /[;\r\n\u0000]/.test(cookie.value)) return null;
    const domain = String(cookie.domain || "").replace(/^\./, "").toLowerCase();
    if (!domain || /[^a-z0-9.:-]/.test(domain)) return null;
    if (cookie.isHostOnly ? domain !== host : (domain !== host && !host.endsWith("." + domain))) return null;
    const path = String(cookie.path || "/");
    if (!path.startsWith("/") || /[;\r\n\u0000]/.test(path)) return null;
    if (cookie.isSecure && protocol !== "https:") return null;
    if (cookie.name.startsWith("__Host-") && (!cookie.isHostOnly || !cookie.isSecure || path !== "/")) return null;
    if (cookie.name.startsWith("__Secure-") && !cookie.isSecure) return null;
    const expiry = Number(cookie.expiresEpochMillis);
    if (!Number.isFinite(expiry) || expiry <= now) return null;
    let result = `${cookie.name}=${cookie.value}; Path=${path}; Max-Age=${Math.floor((expiry - now) / 1000)}`;
    if (!cookie.isHostOnly) result += `; Domain=${domain}`;
    if (cookie.isSecure) result += "; Secure";
    if (cookie.isHttpOnly) result += "; HttpOnly";
    const sameSite = String(cookie.sameSite || "").toLowerCase();
    if (["lax", "strict", "none"].includes(sameSite)) result += `; SameSite=${sameSite}`;
    else if (sameSite === "no_restriction") result += "; SameSite=None";
    return result;
  }

  function senderTab(sender, origin) {
    if (!sender || !sender.tab || sender.frameId !== 0 || !Number.isInteger(sender.tab.id)) return null;
    if (originOf(sender.url) !== origin) return null;
    return sender.tab.id;
  }

  function getJar(tabId, origin) {
    if (!jars.has(tabId)) jars.set(tabId, new Map());
    const byOrigin = jars.get(tabId);
    if (!byOrigin.has(origin)) byOrigin.set(origin, new Map());
    return byOrigin.get(origin);
  }

  browser.runtime.onMessage.addListener((message, sender) => {
    if (!message || !message.type) return undefined;
    const origin = originOf(sender && sender.url);
    const tabId = senderTab(sender, origin);
    if (tabId === null || !origin) return undefined;
    if (message.type === "SET_POLICY") {
      const days = Math.max(7, Math.min(MAX_DAYS, Number(message.days) || 365));
      // A tab may later navigate to an unrelated website or load third-party resources.
      // A policy granted for this document must never promote cookies for those origins.
      policies.set(tabId, { enabled: message.enabled === true, days, origin });
      if (message.enabled !== true) jars.delete(tabId);
      return Promise.resolve({ ok: true });
    }
    if (message.type === "GET_CAPTURED_COOKIES") {
      const jar = jars.get(tabId)?.get(origin);
      return Promise.resolve({ cookies: jar ? Array.from(jar.values()) : [] });
    }
    if (message.type === "PREPARE_RESTORE" && Array.isArray(message.cookies)) {
      const target = new URL(origin);
      const headers = message.cookies.slice(0, MAX_COOKIES)
        .map(c => cookieHeader(c, target.hostname.toLowerCase(), target.protocol, Date.now())).filter(Boolean);
      if (!headers.length) return Promise.resolve({ nonce: null });
      const bytes = new Uint8Array(16);
      crypto.getRandomValues(bytes);
      const nonce = Array.from(bytes, n => n.toString(16).padStart(2, "0")).join("");
      restores.set(tabId, { nonce, origin, headers, applied: 0, requestTab: "NOT_SEEN", expires: Date.now() + 10000 });
      return Promise.resolve({ nonce });
    }
    if (message.type === "RESTORE_STATUS") {
      const pending = restores.get(tabId);
      if (!pending || pending.nonce !== message.nonce || pending.origin !== origin) return Promise.resolve({ count: 0 });
      restores.delete(tabId);
      return Promise.resolve({ count: pending.applied, requestTab: pending.requestTab });
    }
    return undefined;
  });

  // Revoke the previous document's policy as soon as a top-level navigation leaves its origin.
  // Otherwise a new page could cause that policy to act on embedded requests to the old site.
  browser.webRequest.onBeforeRequest.addListener(details => {
    // Keep only a coarse, secret-free diagnostic for this one-time request. A nonce
    // never authorizes writing to a different tab, even if a request is unattributed.
    if (details.url.includes("__marble_vault_restore=")) {
      try {
        const request = new URL(details.url);
        const nonce = request.searchParams.get("__marble_vault_restore");
        if (nonce) {
          for (const [tabId, pending] of restores) {
            if (pending.nonce === nonce && pending.origin === request.origin) {
              pending.requestTab = details.tabId < 0 ? "NO_TAB" : details.tabId === tabId ? "EXPECTED_TAB" : "OTHER_TAB";
            }
          }
        }
      } catch (_) {}
    }
    if (details.type !== "main_frame" || details.tabId < 0) return;
    const policy = policies.get(details.tabId);
    if (policy && policy.origin !== originOf(details.url)) {
      policies.delete(details.tabId);
      jars.delete(details.tabId);
      restores.delete(details.tabId);
    }
  }, { urls: ["<all_urls>"] });

  browser.webRequest.onHeadersReceived.addListener(details => {
    const origin = originOf(details.url);
    if (!origin || details.tabId < 0) return {};
    const headers = details.responseHeaders || [];
    const pending = restores.get(details.tabId);
    if (pending && pending.expires >= Date.now() && pending.origin === origin) {
      const request = new URL(details.url);
      if (request.searchParams.get("__marble_vault_restore") === pending.nonce) {
        restores.delete(details.tabId);
        pending.applied = pending.headers.length;
        restores.set(details.tabId, pending);
        return { responseHeaders: headers.concat(pending.headers.map(value => ({ name: "Set-Cookie", value }))) };
      }
    }
    const policy = policies.get(details.tabId);
    if (!policy?.enabled || policy.origin !== origin) return {};
    const jar = getJar(details.tabId, origin);
    const now = Date.now();
    for (const header of headers) {
      if (header.name.toLowerCase() !== "set-cookie" || !header.value) continue;
      const cookie = parseCookie(header.value, details.url, now);
      if (!cookie) continue;
      const key = `${cookie.domain}\n${cookie.path}\n${cookie.name}`;
      if (cookie.expiresEpochMillis !== null && cookie.expiresEpochMillis <= now) {
        jar.delete(key);
        continue;
      }
      // Extend only session cookies, never override explicit server expiry or logout deletions.
      if (cookie.isSessionOnly) {
        const maxAge = policy.days * 86400;
        header.value += `; Max-Age=${maxAge}`;
        cookie.expiresEpochMillis = now + maxAge * 1000;
        cookie.isSessionOnly = false;
      }
      if (jar.size >= MAX_COOKIES && !jar.has(key)) jar.delete(jar.keys().next().value);
      jar.set(key, cookie);
    }
    return { responseHeaders: headers };
  }, { urls: ["<all_urls>"] }, ["blocking", "responseHeaders"]);

  browser.webRequest.onCompleted.addListener(details => {
    if (details.type === "main_frame" && [401, 419, 440].includes(details.statusCode) && details.tabId >= 0) {
      browser.tabs.sendMessage(details.tabId, {
        type: "HTTP_UNAUTHORIZED", url: details.url, statusCode: details.statusCode
      }).catch(() => {});
    }
  }, { urls: ["<all_urls>"] });

  browser.tabs.onRemoved.addListener(tabId => {
    policies.delete(tabId);
    jars.delete(tabId);
    restores.delete(tabId);
  });
})();
