package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.replaceLast

interface DTOAliasGroup : DTOElement {
    val `as`: PsiElement

    val power: PsiElement?

    val original: PsiElement?

    val dollar: PsiElement?

    val arrow: PsiElement?

    val replacement: PsiElement?

    val macros: List<DTOMacro>

    val positiveProps: List<DTOPositiveProp>

    // aliasGroup -> dtoBody
    val containingLClass: LClass<*>?
        get() = (parent as DTODtoBody).containingLClass

    fun apply(value: String): String {
        val isPrefix = power != null
        val isSuffix = dollar != null

        val original = original?.text
        val replacement = replacement?.text

        if (original == null && replacement != null) {
            if (isPrefix) {
                return replacement + value.replaceFirstChar { it.uppercase() }
            }
            if (isSuffix) {
                return value + replacement.replaceFirstChar { it.uppercase() }
            }
            throw AssertionError("Internal bug of JimmerDTO")
        }

        if (isPrefix) {
            if (original != null) {
                return if (replacement == null) {
                    value.removePrefix(original)
                } else {
                    value.replaceFirst(original, replacement)
                }
            }
            throw AssertionError("Internal bug of JimmerDTO")
        }

        if (isSuffix) {
            if (original != null) {
                return if (replacement == null) {
                    value.removeSuffix(original)
                } else {
                    value.replaceLast(original, replacement)
                }
            }
            throw AssertionError("Internal bug of JimmerDTO")
        }

        if (original != null && original in value) {
            return value.replace(original, replacement ?: "")
        } else {
            throw AssertionError("Internal bug of JimmerDTO")
        }
    }
}