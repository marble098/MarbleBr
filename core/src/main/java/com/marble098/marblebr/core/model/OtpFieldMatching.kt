package com.marble098.marblebr.core.model

/**
 * «کد را کجا بگذاریم؟» — انتخابِ دقیقِ کادرِ رمزِ یکبارمصرف در صفحه و ساختنِ اسکریپتِ پرکردن.
 *
 * صفحاتِ کدِ تأیید سه شکلِ رایج دارند و هر سه باید درست پر شوند:
 * ۱. یک کادر با `autocomplete="one-time-code"` یا `maxlength="6"`.
 * ۲. چند کادرِ یک‌رقمی کنارِ هم (کد بینِ کادرها شکسته می‌شود).
 * ۳. چند کادرِ دو‌رقمی، یا یک کادرِ بدونِ هیچ نشانهٔ ظاهری که فقط از روی برچسبش می‌شناسیم.
 *
 * چرا این منطق در `core` و نه در جاوااسکریپت؟ چون انتخابِ اشتباه یعنی «رمزِ بانک در کادرِ
 * جستجو» — یک خطای امنیتی. این‌جا امتیازدهی شفاف و آزمون‌پذیر است و خودِ اسکریپت فقط بعد از
 * انتخابِ قطعی، مقدار را می‌نویسد.
 */

/**
 * توصیفِ یک کادرِ ورودیِ کاندید، همان‌طور که اسکریپتِ کاوش می‌خواند.
 *
 * هیچ مقدارِ کاربر در این ساختار نیست؛ فقط «خالی است یا نه» و ویژگی‌های ظاهری/معنایی.
 */
data class OtpFieldDescriptor(
    val index: Int,
    val type: String = "text",
    val name: String = "",
    val id: String = "",
    val autocomplete: String = "",
    val inputMode: String = "",
    val placeholder: String = "",
    val label: String = "",
    val maxLength: Int = 0,
    val hasValue: Boolean = false,
    val disabled: Boolean = false,
    val readOnly: Boolean = false,
    val visible: Boolean = true,
    val digitsOnly: Boolean = false,
) {
    val isOneTimeCodeAutocomplete: Boolean
        get() = autocomplete.equals("one-time-code", ignoreCase = true)

    val isPassword: Boolean
        get() = type.equals("password", ignoreCase = true) ||
            autocomplete.equals("current-password", ignoreCase = true) ||
            autocomplete.equals("new-password", ignoreCase = true)

    val isNumericKeyboard: Boolean
        get() = inputMode.equals("numeric", ignoreCase = true) ||
            type.equals("tel", ignoreCase = true) ||
            type.equals("number", ignoreCase = true)

    /** همهٔ متن‌هایی که ممکن است معنیِ کادر را نشان دهند، یک‌جا و کوچک‌شده. */
    val searchableText: String
        get() = listOf(name, id, autocomplete, placeholder, label, inputMode)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase()

    fun asJson(): String = buildString {
        append('{')
        append("\"index\":").append(index).append(',')
        append("\"type\":\"").append(JsLiteral.quote(type).removeSurrounding("\"")).append("\",")
        append("\"name\":\"").append(JsLiteral.quote(name).removeSurrounding("\"")).append("\",")
        append("\"id\":\"").append(JsLiteral.quote(id).removeSurrounding("\"")).append("\",")
        append("\"autocomplete\":\"").append(JsLiteral.quote(autocomplete).removeSurrounding("\"")).append("\",")
        append("\"inputMode\":\"").append(JsLiteral.quote(inputMode).removeSurrounding("\"")).append("\",")
        append("\"placeholder\":\"").append(JsLiteral.quote(placeholder).removeSurrounding("\"")).append("\",")
        append("\"label\":\"").append(JsLiteral.quote(label).removeSurrounding("\"")).append("\",")
        append("\"maxLength\":").append(maxLength).append(',')
        append("\"hasValue\":").append(hasValue).append(',')
        append("\"visible\":").append(visible).append(',')
        append("\"digitsOnly\":").append(digitsOnly)
        append('}')
    }

    companion object {
        /**
         * پارسِ خروجیِ [OtpFieldProbe.DISCOVER_SCRIPT].
         *
         * قالبِ خروجی یک شیءِ **تخت** با کلیدهای پیشوندی است (`count`, `f0n`, `f0ml`, …) تا با
         * همان پارسرِ سخت‌گیرِ [FlatJson] خوانده شود و لازم نباشد در `core` یک پارسرِ JSONِ
         * تودرتو و آزمون‌نشده اضافه کنیم.
         */
        fun parseAll(raw: String?): List<OtpFieldDescriptor> {
            val flat = FlatJson.parse(raw) ?: return emptyList()
            val count = flat.int("count").coerceIn(0, 32)
            return (0 until count).mapNotNull { index ->
                val prefix = "f$index"
                val maxLength = flat.int("${prefix}ml")
                OtpFieldDescriptor(
                    index = flat.int("${prefix}ix").takeIf { it > 0 } ?: index,
                    type = flat.string("${prefix}t").ifBlank { "text" },
                    name = flat.string("${prefix}n"),
                    id = flat.string("${prefix}i"),
                    autocomplete = flat.string("${prefix}ac"),
                    inputMode = flat.string("${prefix}im"),
                    placeholder = flat.string("${prefix}pl"),
                    label = flat.string("${prefix}lb"),
                    maxLength = maxLength,
                    hasValue = flat.bool("${prefix}v"),
                    disabled = flat.bool("${prefix}dis"),
                    readOnly = flat.bool("${prefix}ro"),
                    visible = flat.bool("${prefix}vis"),
                    digitsOnly = flat.bool("${prefix}dg"),
                )
            }
        }
    }
}

/** چگونگیِ پرکردن: یک کادر، یا چند کادرِ شکسته. */
sealed interface OtpFillPlan {
    val confidence: Int

    data class Single(val fieldIndex: Int, override val confidence: Int, val fieldName: String = "") : OtpFillPlan

    /** کدِ چند‌رقمی بینِ چند کادر تقسیم می‌شود؛ `digits` رقمِ هر کادر است. */
    data class Split(
        val fieldIndexes: List<Int>,
        val digitsPerField: Int,
        override val confidence: Int,
    ) : OtpFillPlan

    /** کد از قبل در کادرها هست؛ کارِ دوباره لازم نیست. */
    data object AlreadyFilled : OtpFillPlan {
        override val confidence: Int get() = 100
    }

    data class NoMatch(val reason: String) : OtpFillPlan {
        override val confidence: Int get() = 0
    }

    val isActionable: Boolean
        get() = this is Single || this is Split
}

object OtpFieldMatcher {

    /** حداقلِ امتیاز برای اینکه جسارتِ نوشتن در کادر را داشته باشیم. */
    const val MIN_CONFIDENCE = 55

    private val STRONG_HINTS = listOf(
        "otp", "one-time", "one time", "onetime", "verification", "verify", "verif",
        "sms-code", "smscode", "sms_code", "security-code", "securitycode", "auth-code",
        "authcode", "auth-code", "login-code", "logincode", "token", "passcode", "twofactor",
        "two-factor", "totp", "otac", "mfa", "2fa", "code-number",
    )

    private val LOCALIZED_HINTS = listOf(
        "کد", "رمز", "تایید", "تأیید", "تاییید", "فعال", "پیامک", "اس ام اس", "پویا", "احراز",
        "یکبار مصرف", "یک‌بار مصرف", "موقت", "امنیتی",
    )

    private val WEAK_HINTS = listOf("pin", "number", "digit", "verify", "confirm")

    /** نشانه‌هایی که یعنی این کادر قطعاً جایِ چیزِ دیگری است. */
    private val DISQUALIFIERS = listOf(
        "password", "passwd", "pass", "email", "e-mail", "username", "user-name", "user",
        "login-name", "phone", "mobile", "tel-number", "search", "query", "amount", "price",
        "card", "cvv", "cvc", "address", "captcha", "newsletter", "coupon", "promo", "referral",
        "نام", "ایمیل", "کاربری", "تلفن", "موبایل", "جستجو", "مبلغ", "کارت", "آدرس", "کپچا",
    )

    fun plan(fields: List<OtpFieldDescriptor>, code: String): OtpFillPlan {
        val digits = code.filter(Char::isDigit)
        if (digits.length < OneTimeCodeExtractor.MIN_DIGITS) return OtpFillPlan.NoMatch("CODE_TOO_SHORT")
        val usable = fields.filter { it.visible && !it.disabled && !it.readOnly && !it.isPassword }
        if (usable.isEmpty()) return OtpFillPlan.NoMatch("NO_FIELDS")

        // کد از قبل همان‌جا نشسته است؟ (کاربر خودش پر کرده یا جای‌گذاری کرده)
        val already = usable.any { it.hasValue && it.maxLength == digits.length }
        val splitGroup = bestSplitGroup(usable, digits)
        val single = bestSingleField(usable, digits)

        if (splitGroup != null && (single == null || splitGroup.confidence >= single.confidence)) {
            return OtpFillPlan.Split(
                fieldIndexes = splitGroup.fields.map { it.index },
                digitsPerField = splitGroup.digitsPerField,
                confidence = splitGroup.confidence,
            )
        }
        if (single != null && single.confidence >= MIN_CONFIDENCE) {
            return OtpFillPlan.Single(single.field.index, single.confidence, single.field.name.ifBlank { single.field.id })
        }
        if (already) return OtpFillPlan.AlreadyFilled
        return OtpFillPlan.NoMatch(single?.let { "LOW_CONFIDENCE" } ?: "NO_OTP_FIELD")
    }

    /**
     * امتیازِ یک کادرِ تنها.
     *
     * وزن‌ها عمداً ساده و قابلِ توضیح‌اند: نشانهٔ صریحِ `autocomplete` قوی‌ترین شاهد است،
     * سپس کلیدواژه در نام/شناسه/برچسب، سپس طولِ منطبق و کیبوردِ عددی. هر نشانهٔ «جایِ چیزِ
     * دیگر» کادر را از دور خارج می‌کند (منفیِ سنگین)، نه اینکه فقط امتیازش را کم کند.
     */
    fun scoreSingleField(field: OtpFieldDescriptor, digits: String): Int {
        val text = field.searchableText
        if (DISQUALIFIERS.any { dis -> text.contains(dis) }) return 0
        if (field.isPassword) return 0
        // کادری با maxlength کوچک‌تر از طولِ کد، نمی‌تواند کلِ کد را نگه دارد؛ باید بخشی از
        // گروهِ کادرهای شکسته باشد. اگر تنها کاندید باشد، «هیچ» بهتر از نوشتنِ کدِ ناقص است.
        if (field.maxLength > 0 && field.maxLength < digits.length) return 0
        var score = 10
        if (field.isOneTimeCodeAutocomplete) score += 100
        if (STRONG_HINTS.any { text.contains(it) }) score += 60
        if (LOCALIZED_HINTS.any { text.contains(it) }) score += 55
        if (WEAK_HINTS.any { text.contains(it) }) score += 12
        if (field.isNumericKeyboard) score += 16
        when {
            field.maxLength <= 0 -> score += 4
            field.maxLength == digits.length -> score += 30
            kotlin.math.abs(field.maxLength - digits.length) <= 1 -> score += 12
            field.maxLength in 4..8 -> score += 8
            else -> score -= 20
        }
        if (field.digitsOnly) score += 8
        if (field.hasValue) score -= 28
        // کادرِ «کد» در فرمِ ورود (کنارِ گذرواژه) معمولاً کدِ دوم است، نه کدِ پیامکی؛ ولی اگر
        // تنها گزینه باشد هنوز بهتر از هیچ است.
        return score.coerceIn(0, 130)
    }

    private data class ScoredField(val field: OtpFieldDescriptor, val confidence: Int)

    private fun bestSingleField(fields: List<OtpFieldDescriptor>, digits: String): ScoredField? =
        fields.map { ScoredField(it, scoreSingleField(it, digits)) }
            .filter { it.confidence > 0 }
            .maxByOrNull { it.confidence }

    private data class SplitGroup(val fields: List<OtpFieldDescriptor>, val digitsPerField: Int, val confidence: Int)

    /**
     * کدهای شکسته بینِ چند کادر.
     *
     * گروه باید **پیوسته** باشد (کادرهای مجاور در ترتیبِ صفحه) و اندازه‌اش با تعدادِ رقم‌ها
     * بخواند؛ مثلاً ۶ کادرِ یک‌رقمی یا ۳ کادرِ دو‌رقمی برای کدِ ۶ رقمی. یک کادرِ یک‌رقمیِ
     * تنها (مثلِ کادرِ «کدِ ملی») هرگز گروه حساب نمی‌شود.
     */
    private fun bestSplitGroup(fields: List<OtpFieldDescriptor>, digits: String): SplitGroup? {
        val ordered = fields.sortedBy { it.index }
        val candidates = mutableListOf<SplitGroup>()
        var start = 0
        while (start < ordered.size) {
            val first = ordered[start]
            val digitsPerField = inferDigitsPerField(first)
            if (digitsPerField == 0) {
                start++
                continue
            }
            var end = start
            while (end + 1 < ordered.size) {
                val next = ordered[end + 1]
                if (inferDigitsPerField(next) != digitsPerField || next.index != ordered[end].index + 1) break
                end++
            }
            val group = ordered.subList(start, end + 1)
            if (group.size >= 3) {
                val expectedDigits = group.size * digitsPerField
                if (expectedDigits == digits.length) {
                    var score = 70
                    if (group.any { it.isOneTimeCodeAutocomplete }) score += 25
                    if (group.any { field -> STRONG_HINTS.any { field.searchableText.contains(it) } }) score += 15
                    if (group.any { field -> LOCALIZED_HINTS.any { field.searchableText.contains(it) } }) score += 12
                    if (group.none { it.hasValue }) score += 8
                    candidates += SplitGroup(group, digitsPerField, score.coerceAtMost(100))
                }
            }
            start = end + 1
        }
        return candidates.maxByOrNull { it.confidence }
    }

    /** تعدادِ رقمی که یک کادرِ «تک‌رقمی/دو‌رقمی» انتظار دارد؛ `0` یعنی کادرِ گروه نیست. */
    private fun inferDigitsPerField(field: OtpFieldDescriptor): Int = when (field.maxLength) {
        1 -> 1
        2 -> 2
        3 -> 3
        else -> 0
    }
}

/**
 * اسکریپت‌های کاوش و پرکردنِ کدِ یکبارمصرف.
 *
 * هر کادرِ کاندید یک نشانهٔ پایدار (`data-marble-otp`) می‌گیرد؛ انتخاب در Kotlin انجام می‌شود و
 * اسکریپتِ پرکردن فقط همان نشانه‌ها را هدف می‌گیرد. این‌طور انتخابگرِ شکننده (`input[3]`) نداریم
 * و اگر صفحه بینِ کاوش و پرکردن بازچینش شود، همان کادرِ درست پر می‌شود.
 */
object OtpFieldProbe {

    const val TAG_ATTRIBUTE = "data-marble-otp"

    /** کاوشِ کادرهای کد؛ خروجیِ تخت با کلیدهای پیشوندی. */
    val DISCOVER_SCRIPT: String = """
(function () {
  try {
    var visible = function (el) {
      if (!el || !el.getClientRects) return false;
      if (!(el.getClientRects().length || el.offsetParent)) return false;
      var style = window.getComputedStyle ? window.getComputedStyle(el) : null;
      if (style && (style.visibility === 'hidden' || style.display === 'none')) return false;
      return true;
    };
    var labelOf = function (el) {
      try {
        if (el.labels && el.labels.length) return (el.labels[0].innerText || '').trim();
        var id = el.getAttribute('id');
        if (id) {
          var lab = document.querySelector('label[for="' + id.replace(/"/g, '') + '"]');
          if (lab) return (lab.innerText || '').trim();
        }
        var parent = el.closest('label');
        if (parent) return (parent.innerText || '').trim();
        var described = el.getAttribute('aria-labelledby');
        if (described) {
          var target = document.getElementById(described);
          if (target) return (target.innerText || '').trim();
        }
      } catch (e) {}
      return '';
    };
    var clip = function (value, max) {
      var text = String(value == null ? '' : value).replace(/\s+/g, ' ').trim();
      return text.length > max ? text.slice(0, max) : text;
    };
    var all = [].slice.call(document.querySelectorAll('input'));
    var out = [];
    var previousTag = null;
    for (var i = 0; i < all.length && out.length < 32; i++) {
      var el = all[i];
      var type = (el.getAttribute('type') || 'text').toLowerCase();
      if (type === 'password' || type === 'hidden' || type === 'email' || type === 'file' || type === 'checkbox' || type === 'radio') continue;
      var autocomplete = (el.getAttribute('autocomplete') || '').toLowerCase();
      var maxLength = parseInt(el.getAttribute('maxlength') || '0', 10) || 0;
      var name = clip(el.getAttribute('name'), 60);
      var id = clip(el.getAttribute('id'), 60);
      var placeholder = clip(el.getAttribute('placeholder'), 60);
      var label = clip(labelOf(el), 60);
      var haystack = (name + ' ' + id + ' ' + placeholder + ' ' + label + ' ' + autocomplete).toLowerCase();
      var looksLikeOtp = autocomplete === 'one-time-code' || maxLength === 1 || maxLength === 2 ||
        /otp|one[-_ ]?time|verif|sms|token|passcode|auth|pin|2fa|mfa|code|کد|رمز|تایید|تأیید|فعال|پویا|پیامک/.test(haystack);
      if (!looksLikeOtp) { previousTag = null; continue; }
      var index = out.length + 1;
      try { el.setAttribute('${TAG_ATTRIBUTE}', String(index)); } catch (e) {}
      out.push({
        index: index,
        type: type,
        name: name,
        id: id,
        autocomplete: autocomplete,
        inputMode: (el.getAttribute('inputmode') || '').toLowerCase(),
        placeholder: placeholder,
        label: label,
        maxLength: maxLength,
        hasValue: !!el.value,
        disabled: !!el.disabled,
        readOnly: !!el.readOnly,
        visible: visible(el),
        digitsOnly: /[0-9]|\\d/.test(el.getAttribute('pattern') || '') || type === 'tel' || /numeric/.test(el.getAttribute('inputmode') || '')
      });
      previousTag = index;
    }
    var flat = { count: out.length };
    for (var k = 0; k < out.length; k++) {
      var prefix = 'f' + k;
      var item = out[k];
      flat[prefix + 'ix'] = item.index;
      flat[prefix + 't'] = item.type;
      flat[prefix + 'n'] = item.name;
      flat[prefix + 'i'] = item.id;
      flat[prefix + 'ac'] = item.autocomplete;
      flat[prefix + 'im'] = item.inputMode;
      flat[prefix + 'pl'] = item.placeholder;
      flat[prefix + 'lb'] = item.label;
      flat[prefix + 'ml'] = item.maxLength;
      flat[prefix + 'v'] = item.hasValue;
      flat[prefix + 'dis'] = item.disabled;
      flat[prefix + 'ro'] = item.readOnly;
      flat[prefix + 'vis'] = item.visible;
      flat[prefix + 'dg'] = item.digitsOnly;
    }
    return JSON.stringify(flat);
  } catch (error) {
    return '{"count":0}';
  }
})()
    """.trimIndent()

    /**
     * اسکریپتِ پرکردن: کد را در کادر(های) انتخاب‌شده می‌نویسد و رویدادهای واقعی می‌فرستد تا
     * فریم‌ورک‌های واکنشی (React/Vue) هم متوجه شوند. نتیجه: `filled`، `already`، `no-field`،
     * `failed`.
     */
    fun fillScript(code: String, plan: OtpFillPlan): String {
        val digits = code.filter(Char::isDigit)
        val targets = when (plan) {
            is OtpFillPlan.Single -> listOf(plan.fieldIndex)
            is OtpFillPlan.Split -> plan.fieldIndexes
            else -> emptyList()
        }
        val digitsPerField = when (plan) {
            is OtpFillPlan.Split -> plan.digitsPerField
            is OtpFillPlan.Single -> digits.length
            else -> 0
        }
        return """
(function () {
  try {
    var digits = ${JsLiteral.quote(digits)};
    var targets = ${targets.joinToString(prefix = "[", postfix = "]") { it.toString() }};
    var perField = $digitsPerField;
    if (!digits || !targets.length) return 'no-field';
    var attr = '${TAG_ATTRIBUTE}';
    var setValue = function (el, value) {
      var descriptor = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
      if (descriptor && descriptor.set) { descriptor.set.call(el, value); } else { el.value = value; }
      el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
      try { el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: String(value).slice(-1) })); } catch (e) {}
    };
    var filled = 0;
    var already = 0;
    for (var i = 0; i < targets.length; i++) {
      var el = document.querySelector('input[' + attr + '="' + targets[i] + '"]');
      if (!el) continue;
      var chunk = perField > 0 ? digits.slice(i * perField, (i + 1) * perField) : digits;
      if (el.value === chunk) { already++; continue; }
      try { el.focus(); } catch (e) {}
      setValue(el, chunk);
      filled++;
    }
    if (!filled && already) return 'already';
    return filled ? 'filled' : 'no-field';
  } catch (error) {
    return 'failed';
  }
})()
        """.trimIndent()
    }

    /** اسکریپتِ کوتاه برای شمردنِ کادرهای کد در کاوشِ سبکِ صفحه. */
    val QUICK_COUNT_SCRIPT: String = """
(function () {
  try {
    var inputs = [].slice.call(document.querySelectorAll('input'));
    var count = 0;
    for (var i = 0; i < inputs.length; i++) {
      var el = inputs[i];
      var type = (el.getAttribute('type') || 'text').toLowerCase();
      if (type === 'password' || type === 'hidden' || type === 'email') continue;
      var ac = (el.getAttribute('autocomplete') || '').toLowerCase();
      var max = parseInt(el.getAttribute('maxlength') || '0', 10) || 0;
      var hay = ((el.getAttribute('name') || '') + ' ' + (el.getAttribute('id') || '') + ' ' +
        (el.getAttribute('placeholder') || '') + ' ' + ac).toLowerCase();
      if (ac === 'one-time-code' || max === 1 || max === 2 ||
          /otp|one[-_ ]?time|verif|sms|token|passcode|auth|pin|2fa|mfa|code|کد|رمز|تایید|تأیید|فعال|پویا|پیامک/.test(hay)) count++;
    }
    return String(count);
  } catch (error) { return '0'; }
})()
    """.trimIndent()
}
