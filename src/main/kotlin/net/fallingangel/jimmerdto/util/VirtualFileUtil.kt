package net.fallingangel.jimmerdto.util

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.pathString

val VirtualFile.isFile: Boolean
    get() = isValid && !isDirectory

fun VirtualFile.open(project: Project, offset: Int = 0) {
    val openFileDescriptor = OpenFileDescriptor(project, this, offset)
    FileEditorManager.getInstance(project).openEditor(openFileDescriptor, true)
}

fun VirtualFile.findOrCreateFile(relativePath: String): VirtualFile {
    val file = getResolvedVirtualFile(relativePath) { name, isLast ->
        findChild(name) ?: when (isLast) {
            true -> createChildData(fileSystem, name)
            else -> createChildDirectory(fileSystem, name)
        }
    }
    if (!file.isFile) {
        throw IOException("Expected file instead of directory: $path/$relativePath")
    }
    return file
}

fun VirtualFile.findOrCreateDirectory(relativePath: String): VirtualFile {
    val directory = getResolvedVirtualFile(relativePath) { name, _ ->
        findChild(name) ?: createChildDirectory(fileSystem, name)
    }
    if (!directory.isDirectory) {
        throw IOException("Expected directory instead of file: $path/$relativePath")
    }
    return directory
}

fun VirtualFile.findFile(relativePath: String): VirtualFile? {
    val file = findFileOrDirectory(relativePath) ?: return null
    if (!file.isFile) {
        throw IOException("Expected file instead of directory: $path/$relativePath")
    }
    return file
}

fun VirtualFile.findDirectory(relativePath: String): VirtualFile? {
    val directory = findFileOrDirectory(relativePath) ?: return null
    if (!directory.isDirectory) {
        throw IOException("Expected directory instead of file: $path/$relativePath")
    }
    return directory
}

fun VirtualFile.findFileOrDirectory(relativePath: String): VirtualFile? {
    return getResolvedVirtualFile(relativePath) { name, _ ->
        findChild(name) ?: return null // return from findFileOrDirectory
    }
}

private inline fun VirtualFile.getResolvedVirtualFile(
    relativePath: String,
    getChild: VirtualFile.(String, Boolean) -> VirtualFile
): VirtualFile {
    val basePath = Paths.get(FileUtil.toSystemDependentName(path))
    val (normalizedBasePath, normalizedRelativePath) = basePath.getNormalizedBaseAndRelativePaths(relativePath)
    var baseVirtualFile = this
    for (i in normalizedBasePath.nameCount until basePath.nameCount) {
        baseVirtualFile = checkNotNull(baseVirtualFile.parent) {
            "Cannot resolve base virtual file for: $path/$relativePath"
        }
    }
    var virtualFile = baseVirtualFile
    if (normalizedRelativePath.pathString.isNotEmpty()) {
        val names = normalizedRelativePath.map { it.pathString }
        for ((i, name) in names.withIndex()) {
            if (!virtualFile.isDirectory) {
                throw IOException("Expected directory instead of file: ${virtualFile.path}")
            }
            virtualFile = virtualFile.getChild(name, i == names.lastIndex)
        }
    }
    return virtualFile
}
