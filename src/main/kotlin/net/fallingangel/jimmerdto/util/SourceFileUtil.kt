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
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
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
 */
fun VirtualFile.nameIdentifier(project: Project): PsiNameIdentifierOwner? {
    return try {
        if (isJavaOrKotlin) {
            psiClass(project)
        } else {
            ktClass(project)
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
            ktClass(project)?.annotationEntries?.map(KtAnnotationEntry::qualifiedName)
        }
    } catch (e: IllegalFileFormatException) {
        emptyList()
    }
    return annotations ?: emptyList()
}

/**
 * 获取DTO文件对应实体的属性列表
 */
fun VirtualFile.properties(project: Project): List<Property> {
    val classFile = entityFile(project) ?: return emptyList()
    val properties = try {
        if (classFile.isJavaOrKotlin) {
            classFile.psiClass(project)
                    ?.methods
                    ?.map { property ->
                        Property(
                            property.name,
                            property.returnType!!.presentableText,
                            property.annotations.map { it.qualifiedName ?: "" }
                        )
                    }
        } else {
            classFile.ktClass(project)
                    ?.getProperties()
                    ?.map { property ->
                        Property(
                            property.name!!,
                            property.type()!!.toString(),
                            property.annotationEntries.map(KtAnnotationEntry::qualifiedName)
                        )
                    }
        }
    } catch (e: IllegalFileFormatException) {
        null
    }
    return properties ?: emptyList()
}

/**
 * 获取DTO文件对应实体的基类型列表
 */
fun VirtualFile.supers(project: Project): List<String> {
    val classFile = entityFile(project) ?: return emptyList()
    val supers = try {
        if (classFile.isJavaOrKotlin) {
            classFile.psiClass(project)
                    ?.supers
                    ?.map { it.name ?: "" }
                    ?.filter { it != "Object" }
        } else {
            classFile.ktClass(project)
                    ?.superTypeListEntries
                    ?.map(KtSuperTypeListEntry::getText)
        }
    } catch (e: IllegalFileFormatException) {
        null
    }
    return supers ?: emptyList()
}

/**
 * 获取DTO文件对应的实体文件
 */
fun VirtualFile.entityFile(project: Project): VirtualFile? {
    val psiFile = psiFile(project) ?: return null
    val sourcePath = sourceRoot(psiFile)?.path ?: return null

    // sourcePath: src/main/kotlin
    // path: src/main/dto/net/fallingangel/Book.dto
    // pathPrefix: src/main/
    val pathPrefix = sourcePath.commonPrefixWith(path)
    val isJavaOrKotlin = sourcePath.removePrefix(pathPrefix) == "java"

    // net/fallingangel/Book.(java|kt)
    val modelRelativePath = path.removePrefix(pathPrefix)
            .substringAfter('/')
            .replaceAfterLast('.', if (isJavaOrKotlin) "java" else "kt")
    return VirtualFileManager.getInstance()
            .findFileByNioPath(Paths.get("$sourcePath/$modelRelativePath"))
}

/**
 * 获取Java类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.psiClass(project: Project): PsiClass? {
    return psiFile(project)
            ?.clazz<PsiClass>()
}

/**
 * 获取Kotlin类文件中的实体类定义
 *
 * @param isEntity 是否正在获取实体接口的类定义
 */
fun VirtualFile.ktClass(project: Project): KtClass? {
    return psiFile(project)
            ?.clazz<KtClass>()
}

private inline fun <reified T : PsiNameIdentifierOwner> PsiFile.clazz(): T? {
    return PsiTreeUtil.findChildOfType(originalElement, T::class.java)
}

/**
 * 获取KtAnnotationEntry对应注解的全限定名
 */
private val KtAnnotationEntry.qualifiedName: String
    get() {
        // 解析注解条目，获取上下文
        val context = analyze()
        // 获取注解全限定类名
        return context[BindingContext.ANNOTATION, this]?.fqName?.asString() ?: ""
    }
