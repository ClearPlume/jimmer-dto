/**
 * 针对类定义文件的工具方法
 */
package net.fallingangel.jimmerdto.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.exception.IllegalFileFormatException
import net.fallingangel.jimmerdto.structure.Property
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import java.nio.file.Paths

/**
 * true if this file is a java file,
 * false if this file is a kotlin file
 */
val VirtualFile.isJavaOrKotlin: Boolean
    get() {
        val fileType = name.substringAfterLast('.')
        if (fileType !in arrayOf("java", "kt")) {
            throw IllegalFileFormatException(fileType)
        }
        return name.endsWith(".java")
    }

fun VirtualFile.psiFile(project: Project): PsiFile? {
    return PsiManager.getInstance(project)
            .findFile(this)
}

/**
 * 获取类文件中的类名Element
 *
 * @param isEntity 是否正在获取实体接口的类名Element
 */

fun VirtualFile.nameIdentifier(project: Project, isEntity: Boolean = true): PsiNameIdentifierOwner? {
    return try {
        if (isJavaOrKotlin) {
            psiClass(project, isEntity)
        } else {
            ktClass(project, isEntity)
        }
    } catch (e: IllegalFileFormatException) {
        null
    }
}

/**
 * 获取类文件中类的注解，全限定名
 */
fun VirtualFile.annotations(project: Project): List<String> {
    val annotations = try {
        if (isJavaOrKotlin) {
            psiClass(project)?.annotations?.map { it.qualifiedName ?: "" }
        } else {
            ktClass(project)?.annotationEntries?.map {
                // 解析注解条目，获取上下文
                val context = it.analyze()
                // 获取注解信息
                context[BindingContext.ANNOTATION, it]?.fqName?.asString() ?: ""
            }
        }
    } catch (e: IllegalFileFormatException) {
        emptyList()
    }
    return annotations ?: emptyList()
}

/**
 * 获取类文件中类的属性列表
 */
fun VirtualFile.properties(project: Project): List<Property> {
    val psiFile = psiFile(project) ?: return emptyList()
    val sourcePath = sourceRoot(psiFile)?.path ?: return emptyList()

    // sourcePath: src/main/kotlin
    // path: src/main/dto/net/fallingangel/Book.dto
    // pathPrefix: src/main/
    val pathPrefix = sourcePath.commonPrefixWith(path)
    val isJavaOrKotlin = sourcePath.removePrefix(pathPrefix) == "java"

    val modelRelativePath = path.removePrefix(pathPrefix)
            .substringAfter('/')
            .replaceAfterLast('.', if (isJavaOrKotlin) "java" else "kt")
    val classFile = VirtualFileManager.getInstance()
            .findFileByNioPath(Paths.get("$sourcePath/$modelRelativePath"))

    val properties = try {
        if (isJavaOrKotlin) {
            classFile?.psiClass(project)
                    ?.methods
                    ?.map { Property(it.name, it.returnType!!.presentableText) }
        } else {
            classFile?.ktClass(project)
                    ?.getProperties()
                    ?.map { Property(it.name!!, it.type()!!.toString()) }
        }
    } catch (e: IllegalFileFormatException) {
        null
    }
    return properties ?: emptyList()
}

/**
 * 获取Java类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.psiClass(project: Project, isEntity: Boolean = true): PsiClass? {
    return psiFile(project)
            ?.clazz<PsiClass>()
            ?.takeIf { !isEntity || it.isInterface }
}

/**
 * 获取Kotlin类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.ktClass(project: Project, isEntity: Boolean = true): KtClass? {
    return psiFile(project)
            ?.clazz<KtClass>()
            ?.takeIf { !isEntity || it.isInterface() }
}

private inline fun <reified T : PsiNameIdentifierOwner> PsiFile.clazz(): T? {
    return PsiTreeUtil.findChildOfType(originalElement, T::class.java)
}
