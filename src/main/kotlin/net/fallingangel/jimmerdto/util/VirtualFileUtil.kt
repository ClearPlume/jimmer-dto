package net.fallingangel.jimmerdto.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.pathString

fun VirtualFile.psiFile(project: Project): PsiFile? {
    return PsiManager.getInstance(project).findFile(this)
}

/**
 * true if this file is a java file,
 * false if this file is a kotlin file
 */
val VirtualFile.isJavaOrKotlin: Boolean
    get() = name.endsWith(".java")

val VirtualFile.isFile: Boolean
    get() = isValid && !isDirectory

fun VirtualFile.nameIdentifier(project: Project): PsiNameIdentifierOwner? {
    return if (isJavaOrKotlin) {
        psiClass(project)
    } else {
        ktClass(project)
    }
}

fun VirtualFile.annotations(project: Project): List<String> {
    val annotations = if (isJavaOrKotlin) {
        psiClass(project)?.annotations?.map { it.qualifiedName ?: "" }
    } else {
        ktClass(project)?.annotationEntries?.map {
            // 解析注解条目，获取上下文
            val context = it.analyze()
            // 获取注解信息
            context[BindingContext.ANNOTATION, it]?.fqName?.asString() ?: ""
        }
    }
    return annotations ?: emptyList()
}

/**
 * 获取类文件中的实体类定义
 */
fun VirtualFile.psiClass(project: Project): PsiClass? {
    return psiFile(project)?.clazz<PsiClass>()?.takeIf { it.isInterface }
}

/**
 * 获取类文件中的实体类定义
 */
fun VirtualFile.ktClass(project: Project): KtClass? {
    return psiFile(project)?.clazz<KtClass>()?.takeIf { it.isInterface() }
}

private inline fun <reified T : PsiNameIdentifierOwner> PsiFile.clazz(): T? {
    return PsiTreeUtil.findChildOfType(originalElement, T::class.java)
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
