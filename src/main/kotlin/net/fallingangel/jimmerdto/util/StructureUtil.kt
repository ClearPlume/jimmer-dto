package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.prevLeaf
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.ANNOTATION_CLASS_INDEX
import net.fallingangel.jimmerdto.completion.resolve.structure.Structure
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.*
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import javax.swing.Icon
import kotlin.reflect.KClass

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

val PsiClass.isSuperEntity: Boolean
    get() = hasAnnotation(MappedSuperclass::class.qualifiedName!!)

val KtClass.isSuperEntity: Boolean
    get() = hasAnnotation(MappedSuperclass::class.qualifiedName!!)

val PsiClass.isEntity: Boolean
    get() = isSuperEntity || hasAnnotation(Entity::class, Immutable::class)

val PsiClass.isImmutable: Boolean
    get() = isEntity || hasAnnotation(Embeddable::class.qualifiedName!!)

val PsiWhiteSpace.haveUpper: Boolean
    get() = parent is DTOAliasGroup || parent.parent.parent is DTOPositiveProp

val PsiWhiteSpace.upper: PsiElement
    get() = if (parent is DTOAliasGroup) {
        parent
    } else {
        parent.parent.parent
    }

/**
 * 元素是否包含上一层级的属性级结构
 *
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
val PsiElement.haveUpper: Boolean
    get() = parent.parent is DTOAliasGroup || parent.parent is DTOPropBody

/**
 * 元素上一层级的属性级结构
 *
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
val PsiElement.upper: PsiElement
    get() = if (parent.parent is DTOAliasGroup) {
        // aliasGroup
        parent.parent
    } else {
        // positiveProp
        parent.parent.parent
    }

val PsiElement.virtualFile: VirtualFile
    get() = containingFile.originalFile.virtualFile

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

inline fun <reified T : PsiElement> PsiElement.haveParent() = parentOfType<T>() != null

operator fun <S : PsiElement, R, T : Structure<S, R>> S.get(type: T): R {
    return type.value(this)
}

/**
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
fun PsiElement.propPath(): List<String> {
    val propName = when (this) {
        is DTONegativeProp -> name?.value?.let(::listOf) ?: emptyList()

        is DTOPositiveProp -> if (arg == null) {
            listOf(name.value)
        } else {
            if (name.value in listOf("flat", "id")) {
                listOf(arg!!.values.first().text)
            } else {
                emptyList()
            }
        }

        else -> emptyList()
    }

    if (!haveUpper || parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

inline fun <reified P> PsiElement.parent(): P {
    return parent as P
}

inline fun <reified P> PsiElement.parentUnSure(): P? {
    return parent as? P
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
    return allClasses(`package` ?: "").filter { it.isInterface && it.hasAnnotation(Entity::class.qualifiedName!!) }
}

fun Project.allPackages(`package`: String): List<PsiPackage> {
    return JavaPsiFacade.getInstance(this).findPackage(`package`)?.subPackages?.toList() ?: emptyList()
}

/**
 * 获取注解BNF元素对应的PsiClass实例
 */
fun DTOAnnotationName.psiClass(): PsiClass {
    val name = text?.split(".") ?: throw IllegalStateException()
    val clazz = if (name.size == 1) {
        val imports = PsiTreeUtil.findChildrenOfType(containingFile, DTOImportStatement::class.java)
        val import = imports.find { it.qualifiedType.qualifiedName.qualifiedNamePartList.last().text == name.first() }
        import ?: throw IllegalStateException()
        import.qualified
    } else {
        name.joinToString(".")
    }
    return JavaPsiFacade.getInstance(project).findClass(clazz, ProjectScope.getAllScope(project)) ?: throw IllegalStateException()
}

fun <T, Self, Skip, Pattern> PsiElementPattern<T, Self>.withFirstChildSkipping(
    skip: ElementPattern<Skip>,
    pattern: ElementPattern<Pattern>
): Self
        where T : PsiElement,
              Self : PsiElementPattern<T, Self>,
              Skip : PsiElement,
              Pattern : PsiElement {
    return withChildren(
        StandardPatterns.collection(PsiElement::class.java)
                .filter(
                    StandardPatterns.not(skip),
                    StandardPatterns.collection(PsiElement::class.java).first(pattern)
                )
    )
}

fun <T, Self, Pattern> PsiElementPattern<T, Self>.afterLeafExact(pattern: ElementPattern<Pattern>): Self
        where T : PsiElement,
              Self : PsiElementPattern<T, Self>,
              Pattern : PsiElement {
    return with(object : PatternCondition<T>("afterLeafExact") {
        override fun accepts(t: T, context: ProcessingContext): Boolean {
            return pattern.accepts(t.prevLeaf(), context)
        }
    })
}

fun StringPattern.atStart(start: String): StringPattern {
    return with(object : PatternCondition<String>("atStart") {
        override fun accepts(str: String, context: ProcessingContext): Boolean {
            return start.startsWith(str.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
        }
    })
}

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier in modifiers
}

infix fun DTODto.notModifiedBy(modifier: Modifier): Boolean {
    return modifier !in modifiers
}

fun DTOPositiveProp.hasConfig(config: PropConfigName) = configs.any { it.name.text == config.text }

fun KtClass.hasAnnotation(vararg annotations: String) = annotations.any { annotationEntries.map(KtAnnotationEntry::qualifiedName).contains(it) }

fun PsiModifierListOwner.hasAnnotation(vararg anno: KClass<out Annotation>): Boolean {
    return annotations.any { anno.mapNotNull(KClass<out Annotation>::qualifiedName).contains(it.qualifiedName) }
}