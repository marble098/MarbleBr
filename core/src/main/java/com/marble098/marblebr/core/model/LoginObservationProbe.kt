package com.marble098.marblebr.core.model

/**
 * کاوشگرِ صفحهٔ ورود.
 *
 * اسکریپتِ [OBSERVE_SCRIPT] در همان صفحه اجرا می‌شود و فقط چند عدد/بولین برمی‌گرداند؛ نه متنِ
 * صفحه، نه مقدارِ فیلدها. نتیجه در [parse] به [LoginPageObservation] تبدیل می‌شود تا «افزودن هر
 * سایتی» بر اساسِ واقعیتِ همان صفحه تصمیم بگیرد.
 *
 * [AUTOFILL_SCRIPT] تنها جایی است که گذرواژه به صفحه فرستاده می‌شود و فقط پس از تأییدِ کاربر و
 * پاس‌شدنِ «نگهبانِ بدلِ ورود».
 */
object LoginObservationProbe {

    val OBSERVE_SCRIPT: String = """
(function () {
  try {
    var visible = function (el) { return !!(el.getClientRects().length || el.offsetParent); };
    var passwords = [].slice.call(document.querySelectorAll('input[type=password]')).filter(visible);
    var usernames = [].slice.call(document.querySelectorAll(
      'input[type=text],input[type=email],input[type=tel],input[autocomplete=username],input[name=username],input[name=email],input[name=login]'
    )).filter(visible);
    var otp = [].slice.call(document.querySelectorAll(
      'input[autocomplete=one-time-code],input[inputmode=numeric],input[name*=otp i],input[name*=code i],input[id*=otp i],input[id*=code i]'
    )).filter(visible);
    var body = document.body ? (document.body.innerText || '') : '';
    var lowered = body.toLowerCase();
    var frames = [].slice.call(document.querySelectorAll('iframe'));
    var cross = 0, loginFrame = false;
    for (var i = 0; i < frames.length; i++) {
      try {
        var url = new URL(frames[i].src || 'about:blank', location.href);
        if (url.origin !== location.origin && url.protocol.indexOf('http') === 0) {
          cross++;
          if (/login|auth|signin|sign-in|sso|passport|account|oauth/.test(url.href + ' ' + url.hostname)) loginFrame = true;
        }
      } catch (e) {}
    }
    var labels = [].slice.call(document.querySelectorAll('button,a,[role=button],input[type=submit]')).map(function (el) {
      return ((el.innerText || el.value || el.getAttribute('aria-label') || '') + '').toLowerCase();
    }).join('|');
    var refusal = /(مرورگر).{0,20}(پشتیبانی|سازگار)|not supported|unsupported browser|browser is not supported|update your browser|please upgrade/i.test(body);
    var result = {
      passwordFieldCount: passwords.length,
      usernameFieldCount: usernames.length,
      oneTimeCodeFieldCount: otp.length,
      captchaPresent: /captcha|recaptcha|کد امنیتی|ربات نیستم|i'm not a robot/i.test(lowered),
      differentOriginIframeCount: cross,
      loginIframePresent: loginFrame,
      googleButtonPresent: /google|گوگل/.test(labels),
      appleButtonPresent: /apple|اپل/.test(labels),
      passkeyButtonPresent: /passkey|biometric|fingerprint|اثر انگشت|ورود بدون رمز/.test(labels),
      walletButtonPresent: /wallet|کیف پول/.test(labels),
      ssoDomainHint: /single sign|sso|organization|enterprise|سازمانی|شرکتی/.test(lowered),
      magicLinkHint: /magic link|email me a link|لینک ورود|ورود با ایمیل/.test(labels),
      browserRefusalText: refusal,
      javascriptDisabledPage: /enable javascript|جاوااسکریپت را فعال|javascript is required/i.test(lowered),
      lastLoadFailed: false,
      wasThreeTenOrOtpRedirect: /otp|verify|verification|تایید|تأیید|کد یکبار|2fa|mfa/.test(location.href)
    };
    return JSON.stringify(result);
  } catch (error) {
    return '{}';
  }
})()
""".trimIndent()

    /**
     * پرکردنِ فیلدها فقط پس از تأییدِ کاربر.
     * نتیجه: `filled`، `no-form`، `no-password` یا `failed`.
     */
    fun autofillScript(username: String, password: String): String {
        val userLiteral = JsLiteral.quote(username)
        val passLiteral = JsLiteral.quote(password)
        return """
(function () {
  try {
    var marbleUser = $userLiteral;
    var marblePassword = $passLiteral;
    function setValue(el, value) {
      var descriptor = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
      if (descriptor && descriptor.set) { descriptor.set.call(el, value); } else { el.value = value; }
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
      el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
    }
    function visible(el) { return !!(el.getClientRects().length || el.offsetParent); }
    var passwords = [].slice.call(document.querySelectorAll('input[type=password]')).filter(visible);
    if (!passwords.length) return 'no-password';
    var passwordField = passwords[0];
    var ownerForm = passwordField.form || passwordField.closest('form');
    function safeForm() {
      if (!ownerForm) return true;
      var action = new URL(ownerForm.action || location.href, document.baseURI || location.href);
      if (action.protocol !== 'https:' || action.origin !== location.origin) return false;
      var submitters = [].slice.call(ownerForm.querySelectorAll('button[formaction],input[type=submit][formaction],input[type=image][formaction]'));
      for (var i = 0; i < submitters.length; i++) {
        var target = new URL(submitters[i].formAction || submitters[i].getAttribute('formaction') || location.href, document.baseURI || location.href);
        if (target.protocol !== 'https:' || target.origin !== location.origin) return false;
      }
      return true;
    }
    if (!safeForm()) return 'unsafe-form';
    var form = ownerForm || document;
    var usernames = [].slice.call(form.querySelectorAll(
      'input[type=text],input[type=email],input[type=tel],input[autocomplete=username],input[name=username],input[name=email]'
    )).filter(function (el) { return visible(el) && (!ownerForm || !el.form || el.form === ownerForm); });
    if (!usernames.length) return 'no-form';
    setValue(usernames[0], marbleUser);
    if (!safeForm()) return 'unsafe-form';
    setValue(passwordField, marblePassword);
    return 'filled';
  } catch (error) {
    return 'failed';
  }
})()
        """.trimIndent()
    }

    /**
     * خواندنِ مقادیر فرمِ ورود در لحظهٔ ارسال؛ فقط برای پیشنهادِ «ذخیرهٔ این ورود» استفاده می‌شود
     * و مقادیر فقط در حافظهٔ خودِ اپ می‌مانند.
     */
    val CAPTURE_SUBMIT_SCRIPT: String = """
(function () {
  try {
    function visible(el) { return !!(el.getClientRects().length || el.offsetParent); }
    var passwords = [].slice.call(document.querySelectorAll('input[type=password]')).filter(visible);
    if (!passwords.length) return '';
    var password = passwords[0];
    if (!password.value) return '';
    var form = password.closest('form') || document;
    var usernames = [].slice.call(form.querySelectorAll(
      'input[type=text],input[type=email],input[type=tel],input[autocomplete=username],input[name=username],input[name=email]'
    )).filter(visible);
    var user = usernames.length ? (usernames[0].value || '') : '';
    var otp = [].slice.call(document.querySelectorAll('input[autocomplete=one-time-code],input[inputmode=numeric]')).filter(visible);
    return JSON.stringify({ username: user, password: password.value, otpFields: otp.length });
  } catch (error) {
    return '';
  }
})()
""".trimIndent()

    fun parse(raw: String?): LoginPageObservation {
        val flat = FlatJson.parse(raw) ?: return LoginPageObservation.EMPTY
        return LoginPageObservation(
            passwordFieldCount = flat.int("passwordFieldCount"),
            usernameFieldCount = flat.int("usernameFieldCount"),
            oneTimeCodeFieldCount = flat.int("oneTimeCodeFieldCount"),
            captchaPresent = flat.bool("captchaPresent"),
            differentOriginIframeCount = flat.int("differentOriginIframeCount"),
            loginIframePresent = flat.bool("loginIframePresent"),
            googleButtonPresent = flat.bool("googleButtonPresent"),
            appleButtonPresent = flat.bool("appleButtonPresent"),
            passkeyButtonPresent = flat.bool("passkeyButtonPresent"),
            walletButtonPresent = flat.bool("walletButtonPresent"),
            ssoDomainHint = flat.bool("ssoDomainHint"),
            magicLinkHint = flat.bool("magicLinkHint"),
            browserRefusalText = flat.bool("browserRefusalText"),
            javascriptDisabledPage = flat.bool("javascriptDisabledPage"),
            lastLoadFailed = flat.bool("lastLoadFailed"),
            wasThreeTenOrOtpRedirect = flat.bool("wasThreeTenOrOtpRedirect"),
        )
    }

    /** نتیجهٔ اسکریپتِ خواندنِ فرم در لحظهٔ ارسال. */
    data class SubmittedCredentials(val username: String, val password: String, val otpFieldCount: Int = 0)

    fun parseSubmitted(raw: String?): SubmittedCredentials? {
        val flat = FlatJson.parse(raw) ?: return null
        val username = flat.string("username")
        val password = flat.string("password")
        if (password.isEmpty()) return null
        return SubmittedCredentials(username, password, flat.int("otpFields"))
    }
}

/** ساختِ رشتهٔ امنِ جاوااسکریپت از یک مقدارِ دلخواه. */
object JsLiteral {
    fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\'' -> append("\\u0027")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                '/' -> append("\\/")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
        append('"')
    }
}
