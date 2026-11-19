#!/usr/bin/env kotlin

import java.io.File

/**
 * الگوریتم نسخه‌دهی خودکار MarbleBr بر اساس حجم تغییر کد (Diff Size) و تغییرات Breaking.
 *
 * قوانین:
 * - حجم تغییر = مجموع خطوط اضافه‌شده + حذف‌شده در diff، به‌جز فایل‌های lock و generated.
 * - کمتر از ۵۰ خط (< 50): patch++
 * - بین ۵۰ تا ۵۰۰ خط (50..500): minor++ و patch = 0
 * - بیش از ۵۰۰ خط (> 500) یا تغییر breaking: major++ و minor = 0, patch = 0
 * - versionCode = max(currentVersionCode + 1, commitCount + runNumber) (همیشه صعودی)
 */
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(raw: String): SemanticVersion {
            val cleaned = raw.trim().removePrefix("v")
            val parts = cleaned.split(".")
            require(parts.size == 3) { "Invalid semantic version: $raw" }
            return SemanticVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt(),
            )
        }
    }
}

object VersionBumpCalculator {
    private val excludedPatterns = listOf(
        Regex(""".*\.lock$"""),
        Regex(""".*lock\.json$"""),
        Regex(""".*package-lock\.json$"""),
        Regex(""".*yarn\.lock$"""),
        Regex(""".*pnpm-lock\.yaml$"""),
        Regex(""".*gradle\.lockfile$"""),
        Regex(""".*/build/.*"""),
        Regex("""^build/.*"""),
        Regex(""".*/generated/.*"""),
        Regex("""^generated/.*"""),
        Regex(""".*gradle-wrapper\.jar$"""),
        Regex(""".*\.png$"""),
        Regex(""".*\.webp$"""),
        Regex(""".*\.apk$"""),
        Regex(""".*\.aab$"""),
    )

    fun isExcludedFile(path: String): Boolean {
        val normalized = path.trim().replace('\\', '/')
        return excludedPatterns.any { it.matches(normalized) }
    }

    /**
     * Parses `git diff --numstat` output and sums added + deleted lines for non-excluded files.
     */
    fun calculateChangedLinesFromNumstat(numstatOutput: String): Int {
        var total = 0
        for (line in numstatOutput.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(Regex("""\s+"""), limit = 3)
            if (parts.size < 3) continue
            val addedStr = parts[0]
            val deletedStr = parts[1]
            val filePath = parts[2]
            // Binary files show '-' in git diff --numstat
            if (addedStr == "-" || deletedStr == "-") continue
            if (isExcludedFile(filePath)) continue
            val added = addedStr.toIntOrNull() ?: 0
            val deleted = deletedStr.toIntOrNull() ?: 0
            total += (added + deleted)
        }
        return total
    }

    fun hasBreakingIndicator(commitMessage: String): Boolean {
        val msg = commitMessage.trim()
        return msg.contains("BREAKING CHANGE", ignoreCase = true) ||
            msg.contains("BREAKING-CHANGE", ignoreCase = true) ||
            Regex("""^[a-zA-Z]+(\([^)]+\))?!:""").containsMatchIn(msg)
    }

    fun bumpVersion(
        current: SemanticVersion,
        changedLines: Int,
        isBreaking: Boolean = false,
    ): SemanticVersion {
        require(changedLines >= 0) { "changedLines must be non-negative" }
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

    fun computeMonotonicVersionCode(
        previousVersionCode: Int,
        commitCount: Int,
        githubRunNumber: Int?,
    ): Int {
        val candidate = when {
            githubRunNumber != null && githubRunNumber > 0 -> maxOf(commitCount, githubRunNumber)
            else -> commitCount
        }
        return maxOf(previousVersionCode + 1, candidate)
    }
}

fun runCommand(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output
}

fun main(args: Array<String>) {
    val propsFile = File("gradle.properties")
    require(propsFile.exists()) { "gradle.properties not found in working directory" }

    val linesOverride = args.firstOrNull { it.startsWith("--lines=") }
        ?.substringAfter("--lines=")?.toIntOrNull()
    val breakingOverride = args.any { it == "--breaking" }
    val baseRef = args.firstOrNull { it.startsWith("--base=") }
        ?.substringAfter("--base=") ?: "HEAD~1"

    val propsContent = propsFile.readText()
    val currentVersionStr = Regex("""^MARBLE_VERSION_NAME=(.+)$""", RegexOption.MULTILINE)
        .find(propsContent)?.groupValues?.get(1)?.trim() ?: "1.0.0"
    val currentCode = Regex("""^MARBLE_VERSION_CODE=(\d+)$""", RegexOption.MULTILINE)
        .find(propsContent)?.groupValues?.get(1)?.toIntOrNull() ?: 1

    val changedLines = linesOverride ?: run {
        val numstat = runCommand("git", "diff", "--numstat", baseRef, "HEAD")
        VersionBumpCalculator.calculateChangedLinesFromNumstat(numstat)
    }

    val latestCommitMsg = runCommand("git", "log", "-1", "--pretty=%B")
    val isBreaking = breakingOverride || VersionBumpCalculator.hasBreakingIndicator(latestCommitMsg)

    val currentVersion = SemanticVersion.parse(currentVersionStr)
    val nextVersion = VersionBumpCalculator.bumpVersion(currentVersion, changedLines, isBreaking)

    val commitCount = runCommand("git", "rev-list", "--count", "HEAD").trim().toIntOrNull() ?: 1
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
    val nextCode = VersionBumpCalculator.computeMonotonicVersionCode(currentCode, commitCount, runNumber)

    val updatedProps = propsContent
        .replace(Regex("""^MARBLE_VERSION_NAME=.*$""", RegexOption.MULTILINE), "MARBLE_VERSION_NAME=$nextVersion")
        .replace(Regex("""^MARBLE_VERSION_CODE=.*$""", RegexOption.MULTILINE), "MARBLE_VERSION_CODE=$nextCode")

    propsFile.writeText(updatedProps)
    println("Bumped MarbleBr version: $currentVersion ($currentCode) -> $nextVersion ($nextCode) [changedLines=$changedLines, breaking=$isBreaking]")
}

main(args)
