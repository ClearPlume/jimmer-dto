package net.fallingangel.jimmerdto.util

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import org.jetbrains.jps.model.java.JavaSourceRootType

val PsiElement.isJavaOrKotlinSource: Boolean
    get() {
        val sourceRoot = sourceRoot(this)!!.path
        return if ("src/main/java" in sourceRoot) {
            true
        } else if ("src/main/kotlin" in sourceRoot) {
            false
        } else {
            throw IllegalStateException("Cannot determine source type is Java or Kotlin: $sourceRoot")
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
