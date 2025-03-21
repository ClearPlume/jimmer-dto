package net.fallingangel.jimmerdto.util

import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.jvm.JvmModifier
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.siblings
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.ANNOTATION_CLASS_INDEX
import net.fallingangel.jimmerdto.DTOLanguage.xPath
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.JavaNullableType
import org.babyfish.jimmer.sql.Entity
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import javax.swing.Icon
import kotlin.reflect.KClass

val PsiElement.virtualFile: VirtualFile
    get() = containingFile.originalFile.virtualFile

val PsiElement.contentRoot: VirtualFile?
    get() {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        return fileIndex.getContentRootForFile(virtualFile)
    }

val DTOElement.file: DTOFile
    get() = containingFile as DTOFile

val PsiClass.isInSource: Boolean
    get() {
        val fileIndex = ProjectFileIndex.getInstance(project)
        return fileIndex.isInSource(containingFile.virtualFile)
    }

val KotlinType.isInSource: Boolean
    get() {
        val descriptor = toClassDescriptor ?: return false
        val declaration = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtElement ?: return false
        val fileIndex = ProjectFileIndex.getInstance(declaration.project)
        return fileIndex.isInSource(declaration.containingFile.virtualFile)
    }

@Suppress("UnstableApiUsage")
val PsiClass.icon: Icon
    get() = when (language) {
        is JavaLanguage -> when {
            isAnnotationType -> AllIcons.Nodes.Annotationtype
            isInterface -> AllIcons.Nodes.Interface
            isRecord -> AllIcons.Nodes.Record
            isEnum -> AllIcons.Nodes.Enum

            else -> if (hasModifier(JvmModifier.ABSTRACT)) {
                AllIcons.Nodes.AbstractClass
            } else {
                AllIcons.Nodes.Class
            }
        }

        is KotlinLanguage -> (this as KtLightClass).icon

        else -> throw UnsupportedLanguageException("$language is unsupported")
    }

@Suppress("UnstableApiUsage")
val KtLightClass.icon: Icon
    get() = if (kotlinOrigin is KtObjectDeclaration) {
        KotlinIcons.OBJECT
    } else {
        when {
            isAnnotationType -> KotlinIcons.ANNOTATION
            isInterface -> KotlinIcons.INTERFACE
            isEnum -> KotlinIcons.ENUM

            else -> if (hasModifier(JvmModifier.ABSTRACT)) {
                KotlinIcons.ABSTRACT_CLASS
            } else {
                KotlinIcons.CLASS
            }
        }
    }

val PsiType.nullable: Boolean
    get() = presentableText in JavaNullableType.values().map { it.name }

/**
 * 获取KtAnnotationEntry对应注解的全限定名
 */
val KtAnnotationEntry.qualifiedName: String
    get() {
        // 解析注解条目，获取上下文
        val context = analyze()
        // 获取注解全限定类名
        return context[BindingContext.ANNOTATION, this]?.fqName?.asString() ?: ""
    }

inline fun <reified T : PsiElement> PsiElement.findChild(path: String): T {
    return xPath.evaluate(this, xPath.split(path)).toList().first() as T
}

inline fun <reified T : PsiElement> PsiElement.findChildNullable(path: String): T? {
    return xPath.evaluate(this, xPath.split(path)).toList().firstOrNull() as T?
}

inline fun <reified T : PsiElement> PsiElement.findChildren(path: String): List<T> {
    return xPath.evaluate(this, xPath.split(path)).filterIsInstance<T>()
}

inline fun <reified T : PsiElement> PsiElement.sibling(forward: Boolean = true, filter: (PsiElement) -> Boolean): T? {
    return siblings(forward, false)
            .filterIsInstance<T>()
            .firstOrNull(filter)
}

inline fun <reified P> PsiElement.parent(): P {
    return parent as P
}

inline fun <reified P> PsiElement.parentUnSure(): P? {
    return parent as? P
}

fun Project.notification(content: String, type: NotificationType = NotificationType.INFORMATION) {
    NotificationGroupManager.getInstance()
            .getNotificationGroup("JimmerDTO Notification Group")
            .createNotification(content, type)
            .notify(this)
}

fun Project.psiClass(qualifiedName: String): PsiClass? {
    return JavaPsiFacade.getInstance(this).findClass(qualifiedName, ProjectScope.getAllScope(this))
}

fun Project.ktClass(qualifiedName: String): KtClass? {
    val results = KotlinFullClassNameIndex.get(qualifiedName, this, ProjectScope.getAllScope(this))
    return results.toList().takeIf { it.size == 1 }?.get(0) as? KtClass
}

/**
 * @param `package` null等同于空字符串
 */
fun Project.allClasses(`package`: String? = ""): List<PsiClass> {
    val classes = JavaPsiFacade.getInstance(this).findPackage(`package` ?: "")?.classes ?: emptyArray()
    return classes.filter { it !is KtLightClass || (it.kotlinOrigin != null) }
}

/**
 * @param `package` null为获取所有包下所有类
 */
fun Project.allAnnotations(`package`: String? = ""): List<PsiClass> {
    return if (`package` == null) {
        val psiFacade = JavaPsiFacade.getInstance(this)
        val scope = ProjectScope.getAllScope(this)

        FileBasedIndex.getInstance()
                .getAllKeys(ANNOTATION_CLASS_INDEX, this)
                .mapNotNull { psiFacade.findClass(it, scope) }
    } else {
        JavaPsiFacade.getInstance(this).findPackage(`package`)?.classes?.filter { it.isAnnotationType } ?: emptyList()
    }
}

/**
 * @param `package` null等同于空字符串
 */
fun Project.allEntities(`package`: String? = ""): List<PsiClass> {
    return allClasses(`package` ?: "").filter { it.isInterface && it.hasAnnotation(Entity::class) }
}

fun Project.allPackages(`package`: String): List<PsiPackage> {
    return JavaPsiFacade.getInstance(this).findPackage(`package`)?.subPackages?.toList() ?: emptyList()
}

fun PsiModifierListOwner.hasAnnotation(vararg anno: KClass<out Annotation>): Boolean {
    return annotations.any { anno.mapNotNull(KClass<out Annotation>::qualifiedName).contains(it.qualifiedName) }
}

fun KtAnnotated.hasAnnotation(vararg anno: KClass<out Annotation>): Boolean {
    return anno.mapNotNull(KClass<out Annotation>::qualifiedName).any { findAnnotation(FqName(it)) != null }
}