package com.ailihaosi.update

/**
 * Small semver-ish comparator for release tags such as "v0.3.1", "0.3", "0.3.0-beta".
 *
 * - Leading "v"/"V" and whitespace are stripped.
 * - Build metadata after '+' is ignored.
 * - Core is split on '.', each part parsed as its leading digits (missing/garbage = 0).
 * - If cores are equal, a pre-release ("-beta", "-rc1") sorts BEFORE the plain release;
 *   two pre-releases are compared lexicographically (case-insensitive).
 */
object VersionCompare {

    data class Parsed(val core: List<Int>, val preRelease: String?)

    fun normalize(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()

    fun parse(raw: String): Parsed {
        val noBuild = normalize(raw).substringBefore('+')
        val dash = noBuild.indexOf('-')
        val corePart = if (dash >= 0) noBuild.substring(0, dash) else noBuild
        val pre = if (dash >= 0) noBuild.substring(dash + 1).ifBlank { null } else null
        val core = corePart.split('.').map { part ->
            part.trim().takeWhile { it.isDigit() }.take(9).toIntOrNull() ?: 0
        }
        return Parsed(core, pre)
    }

    /** Negative if a < b, 0 if equal, positive if a > b. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        val n = maxOf(pa.core.size, pb.core.size)
        for (i in 0 until n) {
            val x = pa.core.getOrElse(i) { 0 }
            val y = pb.core.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return when {
            pa.preRelease == null && pb.preRelease == null -> 0
            pa.preRelease == null -> 1 // release > pre-release
            pb.preRelease == null -> -1
            else -> pa.preRelease.lowercase().compareTo(pb.preRelease.lowercase())
                .coerceIn(-1, 1)
        }
    }

    /** True only when [remote] is strictly newer than [current]. */
    fun isNewer(remote: String, current: String): Boolean = compare(remote, current) > 0
}
