package com.marble098.marblebr.core.model

/**
 * خوانندهٔ بسیار ساده و ایمنِ یک شیء JSON تخت (فقط کلیدهای ثابت، مقدارهای عدد/بولین/رشته).
 *
 * چرا org.json نه؟ این منطق در ماژول `core` است و باید بدونِ اندروید و بدونِ کتابخانهٔ اضافه
 * قابلِ تست باشد؛ ضمناً سطحِ حمله با پشتیبانی از کلیدهای ناشناخته کوچک‌تر می‌ماند.
 */
class FlatJson private constructor(private val values: Map<String, Any>) {

    fun int(key: String): Int = when (val value = values[key]) {
        is Int -> value
        is Double -> value.toInt()
        is Boolean -> if (value) 1 else 0
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    fun bool(key: String): Boolean = when (val value = values[key]) {
        is Boolean -> value
        is Int -> value != 0
        is Double -> value != 0.0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> false
    }

    fun string(key: String): String = when (val value = values[key]) {
        is String -> value
        is Int -> value.toString()
        is Double -> value.toString()
        is Boolean -> value.toString()
        else -> ""
    }

    companion object {
        fun parse(raw: String?): FlatJson? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
            val values = mutableMapOf<String, Any>()
            var index = 1
            val end = trimmed.lastIndex
            while (index < end) {
                index = skipWhitespace(trimmed, index, end)
                if (index >= end) break
                if (trimmed[index] != '"') return null
                val keyResult = readString(trimmed, index, end) ?: return null
                index = skipWhitespace(trimmed, keyResult.second, end)
                if (index >= end || trimmed[index] != ':') return null
                index = skipWhitespace(trimmed, index + 1, end)
                val valueResult = readValue(trimmed, index, end) ?: skipUntilSeparator(trimmed, index, end) ?: return null
                values[keyResult.first] = valueResult.first
                index = skipWhitespace(trimmed, valueResult.second, end)
                if (index < end && trimmed[index] == ',') index++ else if (index < end) return null
            }
            return FlatJson(values)
        }

        private fun skipWhitespace(text: String, from: Int, end: Int): Int {
            var index = from
            while (index < end && text[index].isWhitespace()) index++
            return index
        }

        private fun readString(text: String, start: Int, end: Int): Pair<String, Int>? {
            if (start >= end || text[start] != '"') return null
            val builder = StringBuilder()
            var index = start + 1
            while (index < end) {
                val char = text[index]
                when {
                    char == '"' -> return builder.toString() to index + 1
                    char == '\\' -> {
                        if (index + 1 >= end) return null
                        when (val escape = text[index + 1]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 5 >= end) return null
                                val code = text.substring(index + 2, index + 6).toIntOrNull(16) ?: return null
                                builder.append(code.toChar())
                                index += 4
                            }
                            else -> builder.append(escape)
                        }
                        index += 2
                    }
                    else -> {
                        builder.append(char)
                        index++
                    }
                }
            }
            return null
        }

        /** مقادیرِ تودرتو (آرایه/شیء) خوانده نمی‌شوند؛ فقط تا کامای سطحِ اول رد می‌شوند. */
        private fun skipUntilSeparator(text: String, start: Int, end: Int): Pair<Any, Int>? {
            var depth = 0
            var index = start
            var inString = false
            while (index < end) {
                val char = text[index]
                when {
                    inString && char == '\\' -> index++
                    char == '"' -> inString = !inString
                    !inString && (char == '{' || char == '[') -> depth++
                    !inString && (char == '}' || char == ']') -> {
                        if (depth == 0) return null
                        depth--
                        if (depth == 0) return "" to index + 1
                    }
                    !inString && char == ',' && depth == 0 -> return "" to index
                }
                index++
            }
            return "" to index
        }

        private fun readValue(text: String, start: Int, end: Int): Pair<Any, Int>? {
            if (start >= end) return null
            val char = text[start]
            if (char == '"') return readString(text, start, end)
            if (text.startsWith("true", start)) return true to start + 4
            if (text.startsWith("false", start)) return false to start + 5
            if (text.startsWith("null", start)) return "" to start + 4
            var index = start
            if (index < end && (text[index] == '-' || text[index] == '+')) index++
            val numberStart = index
            while (index < end && (text[index].isDigit() || text[index] == '.' || text[index] == 'e' || text[index] == 'E' || text[index] == '-')) {
                index++
            }
            if (index == numberStart) return null
            val numeric = text.substring(start, index)
            val asDouble = numeric.toDoubleOrNull() ?: return null
            return if (numeric.contains('.') || numeric.contains('e', true) || numeric.contains('E')) {
                asDouble to index
            } else {
                (asDouble.toInt()) to index
            }
        }
    }
}
