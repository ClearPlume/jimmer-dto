package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.icons.AllIcons
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.patterns.*
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.DTOAnnotationName
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOImport
import net.fallingangel.jimmerdto.psi.DTOModifier
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import javax.swing.Icon

object DTOPsiUtil

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
        val import = imports.find { it.qualifiedType.qualifiedTypeName.qualifiedName.qualifiedNamePartList.last().text == name.first() }
        import ?: throw IllegalStateException()
        import.qualifiedType.text
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

infix fun DTODto.notModifiedBy(modifier: Modifier) = !modifiedBy(modifier)