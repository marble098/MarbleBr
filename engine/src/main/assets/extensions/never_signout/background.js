/* MarbleBr Never Sign-out Engine - Built-in WebExtension Background Script */
(function () {
  "use strict";

  const NATIVE_APP = "marble_never_signout";
  const DEFAULT_EXPIRATION_SECONDS = 365 * 24 * 60 * 60;

  let nativePort = null;
  try {
    nativePort = browser.runtime.connectNative(NATIVE_APP);
    nativePort.onMessage.addListener(handleNativeCommand);
  } catch (err) {
    // Fallback to sendNativeMessage per event
  }

  async function handleNativeCommand(command) {
    if (!command || !command.type) return;

    if (command.type === "RESTORE_COOKIES" && Array.isArray(command.cookies)) {
      for (const c of command.cookies) {
        try {
          const protocol = c.isSecure ? "https://" : "http://";
          const cleanDomain = (c.domain || "").replace(/^\./, "");
          const url = protocol + cleanDomain + (c.path || "/");
          const expirationDate = c.expiresEpochMillis
            ? Math.floor(c.expiresEpochMillis / 1000)
            : Math.floor(Date.now() / 1000) + DEFAULT_EXPIRATION_SECONDS;

          // مقدارِ SameSite در GeckoView به شکل «Lax/Strict/None» است، اما API کوکیِ
          // افزونه «lax/strict/no_restriction» می‌خواهد؛ بدون این نگاشت، درخواست کوکی
          // رد می‌شد و بازیابی بی‌صدا ناقص می‌ماند.
          const sameSiteMap = {
            lax: "lax",
            strict: "strict",
            none: "no_restriction",
            no_restriction: "no_restriction"
          };
          const sameSite = sameSiteMap[String(c.sameSite || "lax").toLowerCase()];

          const details = {
            url: url,
            name: c.name,
            value: c.value,
            domain: c.domain || undefined,
            path: c.path || "/",
            secure: Boolean(c.isSecure),
            httpOnly: Boolean(c.isHttpOnly),
            expirationDate: expirationDate
          };
          if (sameSite) details.sameSite = sameSite;

          await browser.cookies.set(details);
        } catch (e) {
          // Ignore individual cookie set error
        }
      }
    }
  }

  // 1. Automatically promote session cookies to persistent cookies
  if (browser.cookies && browser.cookies.onChanged) {
    browser.cookies.onChanged.addListener(async (changeInfo) => {
      if (changeInfo.removed) {
        if (changeInfo.cause === "explicit" || changeInfo.cause === "expired_overwrite") {
          browser.runtime.sendNativeMessage(NATIVE_APP, {
            event: "COOKIE_REMOVED",
            cookieName: changeInfo.cookie.name,
            domain: changeInfo.cookie.domain,
            storeId: changeInfo.cookie.storeId
          }).catch(() => {});
        }
        return;
      }

      const cookie = changeInfo.cookie;
      if (cookie && cookie.session) {
        const protocol = cookie.secure ? "https://" : "http://";
        const host = cookie.domain.replace(/^\./, "");
        const targetUrl = protocol + host + (cookie.path || "/");
        const futureExpiration = Math.floor(Date.now() / 1000) + DEFAULT_EXPIRATION_SECONDS;

        try {
          await browser.cookies.set({
            url: targetUrl,
            name: cookie.name,
            value: cookie.value,
            domain: cookie.domain,
            path: cookie.path,
            secure: cookie.secure,
            httpOnly: cookie.httpOnly,
            sameSite: cookie.sameSite,
            storeId: cookie.storeId,
            expirationDate: futureExpiration
          });
        } catch (e) {
          // Ignore read-only or synthetic cookies
        }
      }
    });
  }

  // 2. Monitor HTTP 401/419/440 unauthorized responses for Session Health Guard
  if (browser.webRequest && browser.webRequest.onCompleted) {
    browser.webRequest.onCompleted.addListener(
      (details) => {
        if (
          details.type === "main_frame" &&
          (details.statusCode === 401 || details.statusCode === 419 || details.statusCode === 440)
        ) {
          browser.runtime.sendNativeMessage(NATIVE_APP, {
            event: "HTTP_UNAUTHORIZED",
            url: details.url,
            statusCode: details.statusCode
          }).catch(() => {});
        }
      },
      { urls: ["<all_urls>"] }
    );
  }
})();
