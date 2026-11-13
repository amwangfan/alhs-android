package com.ailihaosi.update

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
            pa.preRelease == null -> 1
            pb.preRelease == null -> -1
            else -> pa.preRelease.lowercase().compareTo(pb.preRelease.lowercase())
                .coerceIn(-1, 1)
        }
    }

        fun isNewer(remote: String, current: String): Boolean = compare(remote, current) > 0
}
