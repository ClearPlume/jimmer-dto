package net.fallingangel.jimmerdto.util

import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Path

/**
 * Returns normalised base and relative paths. These paths can be simply joined to
 *
 * For example:
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `../../a/b`,
 *    returns pair with base path = `/1/2/3` and relative path = `a/b`;
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `../..`,
 *    returns pair with base path = `/1/2/3` and empty relative path;
 *  * for [this] = `/1/2/3/4/5` and [relativePath] = `a/b`,
 *    returns pair with base path = `/1/2/3/4/5` and relative path = `a/b`.
 */
fun Path.getNormalizedBaseAndRelativePaths(relativePath: String): Pair<Path, Path> {
    val normalizedPath = getResolvedPath(relativePath)
    var normalizedBasePath = normalize()
    var normalizedBasePathNameCount = 0
    for ((baseName, name) in normalizedBasePath.zip(normalizedPath)) {
        if (baseName != name) {
            break
        }
        normalizedBasePathNameCount++
    }
    for (i in normalizedBasePathNameCount until nameCount) {
        normalizedBasePath = checkNotNull(normalizedBasePath.parent) {
            "Cannot resolve normalized base path for: $this/$relativePath"
        }
    }
    val normalizedRelativePath = normalizedBasePath.relativize(normalizedPath)
    return normalizedBasePath to normalizedRelativePath
}

/**
 * Resolves and normalizes path under [this] path.
 * I.e. resolve joins [this] and [relativePath] using file system separator
 */
fun Path.getResolvedPath(relativePath: String): Path {
    return resolve(FileUtil.toSystemDependentName(relativePath)).normalize()
}
