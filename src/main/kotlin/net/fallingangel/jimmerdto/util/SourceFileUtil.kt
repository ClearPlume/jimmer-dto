/**
 * 针对类定义文件的工具方法
 */
package net.fallingangel.jimmerdto.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * true if this file is a java file,
 * false if this file is a kotlin file
 */
val VirtualFile.isJavaOrKotlin: Boolean
    get() = name.endsWith(".java")

fun VirtualFile.psiFile(project: Project): PsiFile? {
    return PsiManager.getInstance(project).findFile(this)
}

/**
 * 获取Java类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */

fun VirtualFile.nameIdentifier(project: Project, isEntity: Boolean = true): PsiNameIdentifierOwner? {
    return if (isJavaOrKotlin) {
        psiClass(project, isEntity)
    } else {
        ktClass(project, isEntity)
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
 * 获取Java类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.psiClass(project: Project, isEntity: Boolean = true): PsiClass? {
    return psiFile(project)?.clazz<PsiClass>()?.takeIf { !isEntity || it.isInterface }
}

/**
 * 获取Kotlin类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.ktClass(project: Project, isEntity: Boolean = true): KtClass? {
    return psiFile(project)?.clazz<KtClass>()?.takeIf { !isEntity || it.isInterface() }
}

private inline fun <reified T : PsiNameIdentifierOwner> PsiFile.clazz(): T? {
    return PsiTreeUtil.findChildOfType(originalElement, T::class.java)
}
