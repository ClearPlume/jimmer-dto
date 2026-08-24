package net.fallingangel.jimmerdto.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import net.fallingangel.jimmerdto.core.DTOLanguage.xPath
import net.fallingangel.jimmerdto.lsi.LName
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject

val DTOElement.file: DTOFile
    get() = containingFile as DTOFile

val PsiType.nullable: Boolean
    get() = this is PsiClassType && PsiPrimitiveType.getUnboxedType(this) != null

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

context(element: PsiElement)
fun LName.psiClass(): PsiClass? {
    return JavaPsiFacade.getInstance(element.project).findClass(fqName, element.resolveScope)
}

context(element: PsiElement)
fun LName.ktClass(): KtClassOrObject? {
    return KotlinFullClassNameIndex[fqName, element.project, element.resolveScope].firstOrNull()
}

context(element: PsiElement)
fun LName.inheritors(): List<PsiClass> {
    val clazz = psiClass() ?: return emptyList()
    return ClassInheritorsSearch.search(clazz, GlobalSearchScope.projectScope(element.project), false).toList()
}

fun PsiModifierListOwner.hasAnnotation(vararg anno: LName): Boolean {
    val annotations = annotations.mapNotNull(PsiAnnotation::getQualifiedName)
    return anno.map(LName::toClassId).any { it.asFqNameString() in annotations }
}

fun KaAnnotated.hasAnnotation(vararg anno: LName): Boolean {
    return anno.map(LName::toClassId).any { it in annotations }
}

fun PsiElement.equivalentTo(other: PsiElement?): Boolean {
    return manager.areElementsEquivalent(this, other)
}

fun String.replaceLast(oldValue: String, newValue: String): String {
    return if (endsWith(oldValue)) {
        val left = removeSuffix(oldValue)
        left + newValue.replaceFirstChar { it.uppercase() }
    } else {
        this
    }
}
