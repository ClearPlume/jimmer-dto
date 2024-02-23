/**
 * 针对类定义文件的工具方法
 */
package net.fallingangel.jimmerdto.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.completion.resolve.structure.Structure
import net.fallingangel.jimmerdto.exception.IllegalFileFormatException
import net.fallingangel.jimmerdto.psi.DTOExport
import net.fallingangel.jimmerdto.structure.JavaNullableType
import net.fallingangel.jimmerdto.structure.Property
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
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

val PsiElement.virtualFile: VirtualFile
    get() = containingFile.originalFile.virtualFile

operator fun <S : PsiElement, R, T : Structure<S, R>> S.get(type: T): R {
    return type.value(this)
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
fun VirtualFile.properties(project: Project, propPath: List<String> = emptyList()): List<Property> {
    val classFile = entityFile(project) ?: return emptyList()
    val properties = try {
        if (classFile.isJavaOrKotlin) {
            classFile.psiClass(project, propPath)
                    ?.methods()
                    ?.map { property ->
                        val annotatedNullable = property.annotations.any { it.qualifiedName?.substringAfterLast('.') in arrayOf("Null", "Nullable") }
                        val returnType = property.returnType ?: return emptyList()
                        Property(
                            property.name,
                            returnType.presentableText,
                            annotatedNullable || returnType.nullable,
                            property.annotations.map { it.qualifiedName ?: "" }
                        )
                    }
        } else {
            classFile.ktClass(project, propPath)
                    ?.properties()
                    ?.map { property ->
                        val type = property.type()!!
                        Property(
                            property.name!!,
                            type.toString(),
                            type.isMarkedNullable,
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
 * 获取实体类文件中实体类的基类型列表
 */
fun VirtualFile.supers(project: Project): List<String> {
    val supers = try {
        if (isJavaOrKotlin) {
            psiClass(project)
                    ?.supers
                    ?.map { it.name ?: "" }
                    ?.filter { it != "Object" }
        } else {
            ktClass(project)
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
    val export = psiFile.getChildOfType<DTOExport>()

    return if (export != null) {
        val entityName = export.qualifiedType.text
        AllClassesSearch
                .search(ProjectScope.getProjectScope(project), project) { it == entityName.substringAfterLast('.') }
                .find { it.qualifiedName == entityName }
                ?.virtualFile
    } else {
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
        VirtualFileManager.getInstance()
                .findFileByNioPath(Paths.get("$sourcePath/$modelRelativePath"))
    }
}

/**
 * 获取Java类文件中的实体类定义
 *
 * @param propPath 进一步获取[propPath]属性的类型的类定义
 */
fun VirtualFile.psiClass(project: Project, propPath: List<String> = emptyList()): PsiClass? {
    val psiClass = psiFile(project)?.clazz<PsiClass>()
    return if (propPath.isNotEmpty()) {
        psiClass?.prop(propPath, 0)?.returnType?.clazz()
    } else {
        psiClass
    }
}

fun PsiClass.prop(propPath: List<String>, level: Int): PsiMethod? {
    val prop = methods().find { it.name == propPath[level] }
    return if (propPath.lastIndex == level) {
        prop
    } else {
        prop?.returnType?.clazz()?.prop(propPath, level + 1)
    }
}

/**
 * 获取Kotlin类文件中的实体类定义
 *
 * @param propPath 进一步获取[propPath]属性的类型的类定义
 */
fun VirtualFile.ktClass(project: Project, propPath: List<String> = emptyList()): KtClass? {
    val ktClass = psiFile(project)?.clazz<KtClass>()
    return if (propPath.isNotEmpty()) {
        val prop = ktClass?.prop(propPath, 0) ?: return null
        prop.analyze()[BindingContext.TYPE, prop.typeReference]?.clazz()
    } else {
        ktClass
    }
}

fun KtClass.prop(propPath: List<String>, level: Int): KtProperty? {
    val property = properties().find { it.name == propPath[level] } ?: return null
    return if (propPath.lastIndex == level) {
        property
    } else {
        val propertyClass = property.analyze()[BindingContext.TYPE, property.typeReference]?.clazz()
        propertyClass?.prop(propPath, level + 1)
    }
}

val PsiType.nullable: Boolean
    get() = presentableText in JavaNullableType.values().map { it.name }

fun PsiType.clazz(): PsiClass? {
    val generic = PsiUtil.resolveGenericsClassInType(this)
    return if (generic.substitutor == PsiSubstitutor.EMPTY) {
        generic.element
    } else {
        val propTypeParameters = generic.element?.typeParameters ?: return null
        generic.substitutor.substitute(propTypeParameters[0])?.clazz()
    }
}

fun KotlinType.clazz(): KtClass? {
    return if (arguments.isEmpty()) {
        val typeDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return null
        DescriptorToSourceUtils.getSourceFromDescriptor(typeDescriptor) as? KtClass
    } else {
        arguments[0].type.clazz()
    }
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

fun PsiClass.methods(): List<PsiMethod> {
    val supers = interfaces
            .filter { interfaceClass ->
                interfaceClass.annotations.any {
                    it.qualifiedName in listOf(Constant.Annotation.ENTITY, Constant.Annotation.MAPPED_SUPERCLASS)
                }
            }
    return methods.toList() + supers.map { it.methods() }.flatten()
}

fun KtClass.properties(): List<KtProperty> {
    val supers = superTypeListEntries
            .filter { superType ->
                val context = superType.analyze()
                val annotations = context[BindingContext.TYPE, superType.typeReference]?.clazz()?.annotationEntries ?: return@filter false
                annotations.any {
                    it.qualifiedName in listOf(Constant.Annotation.ENTITY, Constant.Annotation.MAPPED_SUPERCLASS)
                }
            }
    return getProperties() + supers.map(KtSuperTypeListEntry::properties).flatten()
}

private val KtSuperTypeListEntry.properties: List<KtProperty>
    get() {
        val context = analyze()
        return context[BindingContext.TYPE, typeReference]?.clazz()?.properties() ?: emptyList()
    }
