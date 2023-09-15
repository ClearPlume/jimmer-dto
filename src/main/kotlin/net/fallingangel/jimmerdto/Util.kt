package net.fallingangel.jimmerdto

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.psi.KtClass

fun generateRoot(element: PsiElement): VirtualFile? {
    val generateRoot by lazy {
        root(element).firstOrNull { file -> "generated-sources" in file.path || "generated" in file.path }
    }
    return generateRoot
}

fun sourceRoot(element: PsiElement): VirtualFile {
    val sourceRoot by lazy {
        root(element)
                .filter { file -> "src" in file.path }
                .random()
    }
    return sourceRoot
}

fun root(element: PsiElement): List<VirtualFile> {
    val roots by lazy {
        val module = ModuleUtil.findModuleForPsiElement(element)!!
        ModuleRootManager
                .getInstance(module)
                .getSourceRoots(JavaSourceRootType.SOURCE)
    }
    return roots
}

fun VirtualFile.psiFile(project: Project): PsiFile? {
    return PsiManager.getInstance(project).findFile(this)
}

/**
 * true if this file is a java file,
 * false if this file is a kotlin file
 */
val VirtualFile.isJavaOrKotlin: Boolean
    get() = name.endsWith(".java")

fun VirtualFile.nameIdentifier(project: Project): PsiNameIdentifierOwner? {
    return if (isJavaOrKotlin) {
        psiClass(project)
    } else {
        ktClass(project)
    }
}

fun VirtualFile.psiClass(project: Project): PsiClass? {
    return psiFile(project)?.clazz()
}

fun VirtualFile.ktClass(project: Project): KtClass? {
    return psiFile(project)?.clazz()
}

private inline fun <reified T : PsiNameIdentifierOwner> PsiFile.clazz(): T? {
    return PsiTreeUtil.findChildOfType(originalElement, T::class.java)
}
