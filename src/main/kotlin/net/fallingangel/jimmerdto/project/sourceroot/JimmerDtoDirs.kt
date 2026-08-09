package net.fallingangel.jimmerdto.project.sourceroot

class JimmerDtoDirs(raw: Map<String, String>) {
    val mainDirs = raw["jimmer.dto.dirs"]?.splitDirs("src/main") ?: listOf("src/main/dto")

    val testDirs = raw["jimmer.dto.testDirs"]?.splitDirs("src/test") ?: listOf("src/test/dto")

    operator fun component1() = mainDirs

    operator fun component2() = testDirs

    override fun toString(): String {
        return "mainDirs: $mainDirs, testDirs: $testDirs"
    }

    private fun String.splitDirs(prefix: String): List<String> {
        val sorted = split(dirSplitRegex)
            .map { it.trim().trim('/') }
            .filter { it.startsWith("$prefix/") }
            .distinct()
            .sortedBy(String::length)
        return sorted.filterIndexed { i, path ->
            sorted.take(i).none { path == it || path.startsWith("$it/") }
        }
    }

    companion object {
        private val dirSplitRegex = "\\s*[,:;]\\s*".toRegex()
    }
}