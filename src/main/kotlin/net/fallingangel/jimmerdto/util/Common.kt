package net.fallingangel.jimmerdto.util

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.exception.IllegalFileFormatException
import org.jetbrains.jps.model.java.JavaSourceRootType

val VirtualFile.language: Language
    get() {
        return when (val fileType = extension) {
            "java" -> Language.Java
            "kt" -> Language.Kotlin
            else -> throw IllegalFileFormatException(fileType ?: "<no-type>")
        }
    }

fun generateRoot(element: PsiElement): VirtualFile? {
    val generateRoot by lazy {
        root(element).firstOrNull { file -> "generated-sources" in file.path || "generated" in file.path }
    }
    return generateRoot
}

fun sourceRoot(element: PsiElement): VirtualFile? {
    val sourceRoot by lazy {
        root(element).firstOrNull { file -> "src" in file.path }
    }
    return sourceRoot
}

fun dtoRoot(element: PsiElement): VirtualFile? {
    val dtoRootPath = sourceRoot(element)?.toNioPath()?.resolveSibling("dto") ?: return null
    return VirtualFileManager.getInstance().findFileByNioPath(dtoRootPath)
}

fun root(element: PsiElement): List<VirtualFile> {
    val roots by lazy {
        val module = ModuleUtil.findModuleForPsiElement(element) ?: return@lazy emptyList()
        ModuleRootManager
                .getInstance(module)
                .getSourceRoots(JavaSourceRootType.SOURCE)
    }
    return roots
}
