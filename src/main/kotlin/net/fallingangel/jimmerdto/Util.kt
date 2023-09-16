package net.fallingangel.jimmerdto

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext

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
