/**
 * 针对类定义文件的工具方法
 */
package net.fallingangel.jimmerdto.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.exception.IllegalFileFormatException
import net.fallingangel.jimmerdto.psi.DTOExportStatement
import net.fallingangel.jimmerdto.structure.JavaNullableType
import net.fallingangel.jimmerdto.structure.Property
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass

/**
 * 获取类文件中类的注解，全限定名
 */
fun VirtualFile.annotations(project: Project): List<String> {
    val annotations = try {
        when (language) {
            Language.Java -> {
                psiClass(project)?.annotations?.map { it.qualifiedName ?: "" }
            }

            Language.Kotlin -> {
                ktClass(project)?.annotationEntries?.map(KtAnnotationEntry::qualifiedName)
            }
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
        when (classFile.language) {
            Language.Java -> {
                classFile.psiClass(project, propPath)
                        ?.methods()
                        ?.map { property ->
                            val annotatedNullable =
                                property.annotations.any { it.qualifiedName?.substringAfterLast('.') in arrayOf("Null", "Nullable") }
                            val returnType = property.returnType ?: return emptyList()
                            Property(
                                property.name,
                                returnType.presentableText,
                                annotatedNullable || returnType.nullable,
                                property.annotations.map { it.qualifiedName ?: "" }
                            )
                        }
            }

            Language.Kotlin -> {
                classFile.ktClass(project, propPath)
                        ?.properties()
                        ?.map { property ->
                            val type = (property.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType!!
                            Property(
                                property.name!!,
                                type.toString(),
                                type.isMarkedNullable,
                                property.annotationEntries.map(KtAnnotationEntry::qualifiedName)
                            )
                        }
            }
        }
    } catch (e: IllegalFileFormatException) {
        null
    }
    return properties ?: emptyList()
}

fun PsiClass.supers(): List<PsiClass> {
    return supers.toList() + supers.map { it.supers.toList() }.flatten()
}

fun KtClass.supers(): List<KtClass> {
    return supers + supers.map { it.supers }.flatten()
}

/**
 * 获取DTO文件对应的实体文件
 */
fun VirtualFile.entityFile(project: Project): VirtualFile? {
    val psiFile = toPsiFile(project) ?: return null
    val export = psiFile.getChildOfType<DTOExportStatement>()

    val entityName = if (export != null) {
        export.qualified
    } else {
        // sourcePath: src/main/kotlin
        val sourcePath = sourceRoot(psiFile)?.path ?: return null

        // path: src/main/dto/net/fallingangel/Book.dto
        // pathPrefix: src/main/
        val pathPrefix = sourcePath.commonPrefixWith(path)

        // net.fallingangel.Book
        path.removePrefix(pathPrefix)
                // 移除『src/main』
                .substringAfter('/')
                // 移除文件后缀
                .substringBefore('.')
                .replace('/', '.')
    }

    return JavaPsiFacade.getInstance(project).findClass(entityName, ProjectScope.getProjectScope(project))?.virtualFile
}

/**
 * 获取Java类文件中的实体类定义
 *
 * @param propPath 进一步获取[propPath]属性的类型的类定义
 */
fun VirtualFile.psiClass(project: Project, propPath: List<String> = emptyList()): PsiClass? {
    val psiClass = toPsiFile(project)?.clazz<PsiClass>()
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
    val ktClass = toPsiFile(project)?.clazz<KtClass>()
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
        val genericType = generic.substitutor.substitute(propTypeParameters[0]) ?: return null

        if (genericType is PsiWildcardType) {
            if (genericType.isBounded) {
                genericType.clazz()
            } else {
                generic.element
            }
        } else {
            genericType.clazz()
        }
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
val KtAnnotationEntry.qualifiedName: String
    get() {
        // 解析注解条目，获取上下文
        val context = analyze()
        // 获取注解全限定类名
        return context[BindingContext.ANNOTATION, this]?.fqName?.asString() ?: ""
    }

fun PsiClass.methods(): List<PsiMethod> {
    val supers = interfaces
            .filter {
                it.hasAnnotation(Entity::class, MappedSuperclass::class)
            }
    return methods.toList() + supers.map { it.methods() }.flatten()
}

fun KtClass.properties(): List<KtProperty> {
    val supers = superTypeListEntries
            .filter { superType ->
                val context = superType.analyze()
                val annotations = context[BindingContext.TYPE, superType.typeReference]?.clazz()?.annotationEntries ?: return@filter false
                annotations.any {
                    it.qualifiedName in listOf(Entity::class, MappedSuperclass::class).mapNotNull(KClass<*>::qualifiedName)
                }
            }
    return getProperties() + supers.map(KtSuperTypeListEntry::properties).flatten()
}

val KtClass.supers: List<KtClass>
    get() {
        return superTypeListEntries
                .mapNotNull {
                    val context = it.analyze()
                    context[BindingContext.TYPE, it.typeReference]?.clazz()
                }
    }

private val KtSuperTypeListEntry.properties: List<KtProperty>
    get() {
        val context = analyze()
        return context[BindingContext.TYPE, typeReference]?.clazz()?.properties() ?: emptyList()
    }