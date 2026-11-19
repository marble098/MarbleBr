package com.marble098.marblebr.core.version

/**
 * الگوریتم نسخه‌دهی خودکار (Semantic Versioning) بر اساس حجم تغییر کد.
 *
 * قوانین:
 * - حجم تغییر = مجموع خطوط اضافه‌شده + حذف‌شده در diff آن merge، به‌جز فایل‌های lock/generated.
 * - کمتر از ۵۰ خط: patch++
 * - بین ۵۰ تا ۵۰۰ خط: minor++ و patch = 0
 * - بیش از ۵۰۰ خط یا تغییر breaking: major++ و بقیه صفر (minor = 0, patch = 0)
 * - versionCode = بیشینه شماره run، تعداد commitها و previousVersionCode + 1 (همیشه صعودی)
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {

    init {
        require(major >= 0 && minor >= 0 && patch >= 0) {
            "Semantic version components must be non-negative: $major.$minor.$patch"
        }
    }

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(raw: String): SemanticVersion {
            val cleaned = raw.trim().removePrefix("v")
            val parts = cleaned.split(".")
            require(parts.size == 3) { "Invalid semantic version format: '$raw'" }
            return SemanticVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt(),
            )
        }
    }
}

object VersionBumpAlgorithm {
    private val excludedFileRegexes = listOf(
        Regex(""".*\.lock$""", RegexOption.IGNORE_CASE),
        Regex(""".*lock\.json$""", RegexOption.IGNORE_CASE),
        Regex(""".*package-lock\.json$""", RegexOption.IGNORE_CASE),
        Regex(""".*yarn\.lock$""", RegexOption.IGNORE_CASE),
        Regex(""".*pnpm-lock\.yaml$""", RegexOption.IGNORE_CASE),
        Regex(""".*gradle\.lockfile$""", RegexOption.IGNORE_CASE),
        Regex("""(^|.*/)build/.*"""),
        Regex("""(^|.*/)generated/.*"""),
        Regex(""".*gradle-wrapper\.jar$""", RegexOption.IGNORE_CASE),
        Regex(""".*\.(png|webp|jpg|jpeg|apk|aab|jks|keystore)$""", RegexOption.IGNORE_CASE),
    )

    fun isExcludedFile(filePath: String): Boolean {
        val normalized = filePath.trim().replace('\\', '/')
        return excludedFileRegexes.any { it.matches(normalized) }
    }

    /**
     * محاسبه مجموع خطوط اضافه‌شده و حذف‌شده از خروجی `git diff --numstat` با حذف فایل‌های lock و generated.
     */
    fun calculateChangedLinesFromNumstat(numstatOutput: String): Int {
        var sum = 0
        for (rawLine in numstatOutput.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val cols = line.split(Regex("""\s+"""), limit = 3)
            if (cols.size < 3) continue
            val addedRaw = cols[0]
            val deletedRaw = cols[1]
            val path = cols[2]
            if (addedRaw == "-" || deletedRaw == "-") continue
            if (isExcludedFile(path)) continue
            val added = addedRaw.toIntOrNull() ?: 0
            val deleted = deletedRaw.toIntOrNull() ?: 0
            sum += (added + deleted)
        }
        return sum
    }

    fun hasBreakingChangeIndicator(commitMessage: String): Boolean {
        val text = commitMessage.trim()
        if (text.isEmpty()) return false
        return text.contains("BREAKING CHANGE", ignoreCase = true) ||
            text.contains("BREAKING-CHANGE", ignoreCase = true) ||
            Regex("""^[a-zA-Z]+(\([^)]+\))?!:""").containsMatchIn(text)
    }

    fun computeNextVersion(
        current: SemanticVersion,
        changedLines: Int,
        isBreaking: Boolean = false,
    ): SemanticVersion {
        require(changedLines >= 0) { "changedLines cannot be negative: $changedLines" }
        return when {
            isBreaking || changedLines > 500 -> SemanticVersion(
                major = current.major + 1,
                minor = 0,
                patch = 0,
            )
            changedLines >= 50 -> SemanticVersion(
                major = current.major,
                minor = current.minor + 1,
                patch = 0,
            )
            else -> SemanticVersion(
                major = current.major,
                minor = current.minor,
                patch = current.patch + 1,
            )
        }
    }

    fun computeNextVersionCode(
        previousVersionCode: Int,
        commitCount: Int,
        githubRunNumber: Int? = null,
    ): Int {
        val externalCounter = maxOf(commitCount, githubRunNumber ?: 0)
        return maxOf(previousVersionCode + 1, externalCounter)
    }
}
