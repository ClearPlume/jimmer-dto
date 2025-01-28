package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.completion.resolve.structure.Structure
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.nio.file.Paths
import javax.swing.Icon

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
 * 元素：宏、属性、负属性、方法等
 * 属性级结构：flat方法、as组等
 */
val PsiElement.haveUpper: Boolean
    get() {
        if (this is DTOFile) {
            return false
        }
        return parent.parent is DTOAliasGroup || parent.parent.parent is DTOPositiveProp
    }

val PsiElement.upper: PsiElement
    get() = if (parent.parent is DTOAliasGroup) {
        parent.parent
    } else {
        parent.parent.parent
    }

val PsiElement.virtualFile: VirtualFile
    get() = containingFile.originalFile.virtualFile

val DTOExport.qualified: String
    get() = qualifiedType.qualifiedName.qualifiedNamePartList
            .filter { it.elementType != TokenType.WHITE_SPACE }
            .joinToString(".", transform = PsiElement::getText)

val DTOImport.qualified: String
    get() = qualifiedType.qualifiedName.qualifiedNamePartList
            .filter { it.elementType != TokenType.WHITE_SPACE }
            .joinToString(".", transform = PsiElement::getText)

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

fun PsiElement.propPath(): List<String> {
    val propName = if (this is DTOPositiveProp) {
        if (propName.text == "flat") {
            listOf(propArgs!!.valueList[0].text)
        } else {
            listOf(propName.text)
        }
    } else if (this is DTOAliasGroup) {
        emptyList()
    } else {
        println("Only support find path for prop, currently finding prop for <${this::class.simpleName}>")
        emptyList()
    }

    if (!haveUpper || parent.parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

inline fun <reified P> PsiElement.parent(): P {
    return parent as P
}

fun DTODto.classFile(): VirtualFile? {
    val dtoName = dtoName.text
    // 获取GenerateSource源码路径
    val generateRoot = generateRoot(this) ?: return null
    val fileManager = VirtualFileManager.getInstance()
    val export = virtualFile.toPsiFile(project)?.getChildOfType<DTOExport>()
    val `package` = export?.`package`

    val dtoPath = if (export != null) {
        /* 获取package关键字定义的dto类路径 */
        val packageDtoPath = `package`?.qualifiedType?.text?.replace('.', '/')
        /* 若没有指定package，则获取export关键字指定的类路径对应的dto类路径 */
        packageDtoPath ?: (export.qualified.substringBeforeLast('.') + ".dto").replace('.', '/')
    } else {
        /* 获取默认的dto文件生成类路径 */
        // 获取dto根路径
        val dtoRoot = dtoRoot(this)?.path ?: return null
        // 获取dto相对dto根路径的路径
        virtualFile.path.removePrefix(dtoRoot).replace(Regex("/(.+)/.+?dto$"), "$1/dto")
    }
    val generateDtoRoot = fileManager.findFileByNioPath(Paths.get("${generateRoot.path}/$dtoPath")) ?: return null
    return generateDtoRoot.children.find { it.name.split('.')[0] == dtoName }
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
    return allClasses(`package` ?: "").filter { it.isInterface && it.hasAnnotation(Constant.Annotation.ENTITY) }
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
        val imports = PsiTreeUtil.findChildrenOfType(containingFile, DTOImport::class.java)
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

fun StringPattern.atStart(start: String): StringPattern {
    return with(object : PatternCondition<String>("atStart") {
        override fun accepts(str: String, context: ProcessingContext): Boolean {
            return start.startsWith(str.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
        }
    })
}

fun DTOModifier.toModifier() = Modifier.valueOf(text.replaceFirstChar { it.titlecase() })

fun List<DTOModifier>.toModifier() = map(DTOModifier::toModifier)

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier in this.modifierList.toModifier()
}