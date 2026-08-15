package net.fallingangel.jimmerdto.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.core.DTOLanguage.xPath
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

val DTOElement.file: DTOFile
    get() = containingFile as DTOFile

val PsiType.nullable: Boolean
    get() = this is PsiClassType && PsiPrimitiveType.getUnboxedType(this) != null

val PsiType?.defaultValue: String
    get() = when (this) {
        is PsiPrimitiveType -> when (this) {
            PsiTypes.byteType() -> "0"
            PsiTypes.shortType() -> "0"
            PsiTypes.intType() -> "0"
            PsiTypes.longType() -> "0L"
            PsiTypes.doubleType() -> "0.0D"
            PsiTypes.floatType() -> "0.0F"
            PsiTypes.booleanType() -> "false"
            PsiTypes.charType() -> "''"
            PsiTypes.nullType() -> "null"
            else -> "void"
        }

        is PsiArrayType -> "[${componentType.defaultValue}]"

        is PsiClassType -> when (canonicalText) {
            "java.lang.String" -> "\"\""
            "java.util.List" -> "[${parameters[0].defaultValue}]"
            "java.util.Set" -> "[${parameters[0].defaultValue}]"
            "java.util.Queue" -> "[${parameters[0].defaultValue}]"
            else -> "null"
        }

        else -> "null"
    }

val Project.stringType: PsiClassType
    get() = PsiClassType.getTypeByName("java.lang.String", this, ProjectScope.getAllScope(this))

val PsiType.extract: PsiType
    get() = when (this) {
        is PsiArrayType -> componentType.extract

        is PsiClassType -> when (rawType().canonicalText) {
            "java.util.List" -> parameters[0].extract
            "java.util.Set" -> parameters[0].extract
            "java.util.Queue" -> parameters[0].extract
            else -> this
        }

        else -> this
    }

val KtClass.javaFqName: String?
    get() = when (val name = fqName?.asString()) {
        // 基本类型
        "kotlin.Short" -> "java.lang.Short"
        "kotlin.Int" -> "java.lang.Integer"
        "kotlin.Long" -> "java.lang.Long"
        "kotlin.Float" -> "java.lang.Float"
        "kotlin.Double" -> "java.lang.Double"
        "kotlin.Boolean" -> "java.lang.Boolean"
        "kotlin.Byte" -> "java.lang.Byte"
        "kotlin.Char" -> "java.lang.Character"

        // 字符串
        "kotlin.String" -> "java.lang.String"

        // 特殊类型
        "kotlin.Any" -> "java.lang.Object"
        "kotlin.Unit" -> "java.lang.Void"
        "kotlin.Nothing" -> null

        // 集合接口
        "kotlin.collections.List" -> "java.util.List"
        "kotlin.collections.Set" -> "java.util.Set"
        "kotlin.collections.Map" -> "java.util.Map"
        "kotlin.collections.Collection" -> "java.util.Collection"
        "kotlin.collections.MutableList" -> "java.util.List"
        "kotlin.collections.MutableSet" -> "java.util.Set"
        "kotlin.collections.MutableMap" -> "java.util.Map"
        "kotlin.collections.MutableCollection" -> "java.util.Collection"

        // 集合实现
        "kotlin.collections.ArrayList" -> "java.util.ArrayList"
        "kotlin.collections.HashSet" -> "java.util.HashSet"
        "kotlin.collections.LinkedHashSet" -> "java.util.LinkedHashSet"
        "kotlin.collections.HashMap" -> "java.util.HashMap"
        "kotlin.collections.LinkedHashMap" -> "java.util.LinkedHashMap"

        // 数组类型（需特殊处理）
        "kotlin.Array" -> null

        null -> null

        // 自定义或未知类型
        else -> name
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

fun Project.notification(content: String, type: NotificationType = NotificationType.INFORMATION) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("JimmerDTO Notification Group")
        .createNotification(content, type)
        .notify(this)
}

fun PsiElement.psiClass(qualifiedName: String): PsiClass? {
    return JavaPsiFacade.getInstance(project).findClass(qualifiedName, resolveScope)
}

fun PsiElement.ktClass(qualifiedName: String): KtClassOrObject? {
    return KotlinFullClassNameIndex[qualifiedName, project, resolveScope].firstOrNull()
}

fun PsiModifierListOwner.hasAnnotation(vararg anno: ClassId): Boolean {
    val annotations = annotations.mapNotNull(PsiAnnotation::getQualifiedName)
    return anno.any { it.asFqNameString() in annotations }
}

fun KaAnnotated.hasAnnotation(vararg anno: ClassId): Boolean {
    return anno.any { it in annotations }
}

fun Project.literalType(literal: String): PsiType? {
    if (literal.isBlank()) {
        return null
    }
    return PsiElementFactory.getInstance(this).createExpressionFromText(literal, null).type
}

fun String.replaceLast(oldValue: String, newValue: String): String {
    return if (endsWith(oldValue)) {
        val left = removeSuffix(oldValue)
        left + newValue.replaceFirstChar { it.uppercase() }
    } else {
        this
    }
}

fun unreachable(message: String = "Must be unreachable"): Nothing {
    throw IllegalStateException(message)
}
