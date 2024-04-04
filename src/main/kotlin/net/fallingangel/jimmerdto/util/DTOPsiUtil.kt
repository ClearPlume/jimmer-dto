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
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.DTODto
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
            isInterface -> AllIcons.Nodes.Interface
            isRecord -> AllIcons.Nodes.Record
            isAnnotationType -> AllIcons.Nodes.Annotationtype
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
            isInterface -> KotlinIcons.INTERFACE
            isAnnotationType -> KotlinIcons.ANNOTATION
            isEnum -> KotlinIcons.ENUM

            else -> if (hasModifier(JvmModifier.ABSTRACT)) {
                KotlinIcons.ABSTRACT_CLASS
            } else {
                KotlinIcons.CLASS
            }
        }
    }

fun Project.allClasses(`package`: String = ""): List<PsiClass> {
    val classes = JavaPsiFacade.getInstance(this).findPackage(`package`)?.classes ?: emptyArray()
    return classes.filter { it !is KtLightClass || (it.kotlinOrigin != null) }
}

fun Project.allEntities(`package`: String = ""): List<PsiClass> {
    return allClasses(`package`).filter { it.isInterface && it.hasAnnotation(Constant.Annotation.ENTITY) }
}

fun Project.allPackages(`package`: String): List<PsiPackage> {
    return JavaPsiFacade.getInstance(this).findPackage(`package`)?.subPackages?.toList() ?: emptyList()
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

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier.value in this.dtoModifierList.map { it.text }
}

infix fun DTODto.notModifiedBy(modifier: Modifier) = !modifiedBy(modifier)