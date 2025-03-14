package net.fallingangel.jimmerdto.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.siblings
import net.fallingangel.jimmerdto.DTOLanguage.xPath
import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.exception.IllegalFileFormatException
import net.fallingangel.jimmerdto.psi.DTOExportStatement
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext

val VirtualFile.language: Language
    get() {
        return when (val fileType = extension) {
            "java" -> Language.Java
            "kt" -> Language.Kotlin
            else -> throw IllegalFileFormatException(fileType ?: "<no-type>")
        }
    }

val DTOElement.file: DTOFile
    get() = containingFile as DTOFile

val DTOElement.root: VirtualFile?
    get() {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        return fileIndex.getContentRootForFile(virtualFile)
    }

val DTOElement.fqe: String
    get() {
        // src/main绝对路径
        val root = root?.path ?: throw IllegalStateException("Source root is null")
        val export = containingFile.getChildOfType<DTOExportStatement>()

        // dto文件对应类全限定名
        return export?.qualified
            ?: containingFile.virtualFile.path // dto文件
                    // dto文件相对根路径的子路径
                    .removePrefix("$root/")
                    // 移除『dto/』前缀
                    .substringAfter('/')
                    // 移除『.dto』后缀
                    .substringBeforeLast('.')
                    .replace('/', '.')
    }

val DTOElement.fqeClass: PsiClass?
    get() = JavaPsiFacade.getInstance(project).findClass(fqe, ProjectScope.getAllScope(project))

fun Project.psiClass(qualifiedName: String): PsiClass? {
    return JavaPsiFacade.getInstance(this).findClass(qualifiedName, ProjectScope.getAllScope(this))
}

fun Project.ktClass(qualifiedName: String): KtClass? {
    val results = KotlinFullClassNameIndex.get(qualifiedName, this, ProjectScope.getContentScope(this))
    return results.toList().takeIf { it.size == 1 }?.get(0) as? KtClass
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

/**
 * 从PsiClass定义中，依据字段路径寻找它的Psi元素
 *
 * @param propPath 字段路径
 */
fun PsiClass.element(propPath: List<String>): PsiElement? {
    if (propPath.isEmpty()) {
        return null
    }
    return if (language == KotlinLanguage.INSTANCE) {
        val ktClass = PsiTreeUtil.findChildOfType(containingFile.virtualFile.toPsiFile(project), KtClass::class.java) ?: return null
        var e = ktClass.properties().find { it.name == propPath[0] } ?: return null
        propPath
                .drop(1)
                .forEach {
                    e = e.analyze()[BindingContext.TYPE, e.typeReference]
                            ?.clazz()
                            ?.getProperties()
                            ?.find { property -> property.name == it } ?: return null
                }
        e
    } else {
        var e = methods().find { it.name == propPath[0] } ?: return null
        propPath
                .drop(1)
                .forEach {
                    e = e.returnType
                            ?.clazz()
                            ?.methods
                            ?.find { method -> method.name == it } ?: return null
                }
        e
    }
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

fun Project.notification(content: String, type: NotificationType = NotificationType.INFORMATION) {
    NotificationGroupManager.getInstance()
            .getNotificationGroup("JimmerDTO Notification Group")
            .createNotification(content, type)
            .notify(this)
}