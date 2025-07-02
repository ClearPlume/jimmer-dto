package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath
import net.fallingangel.jimmerdto.util.replaceLast

interface DTOAliasGroup : DTOElement {
    val `as`: PsiElement

    val power: PsiElement?

    val original: PsiElement?

    val dollar: PsiElement?

    val arrow: PsiElement

    val replacement: PsiElement?

    val macros: List<DTOMacro>

    val positiveProps: List<DTOPositiveProp>

    fun allSiblings(withSelf: Boolean = false): List<LProperty<*>> {
        val propPath = propPath().dropLast(if (withSelf) 0 else 1)
        return if (propPath.isEmpty()) {
            file.clazz.allProperties
        } else {
            val parentClazz = file.clazz.findPropertyOrNull(propPath)?.actualType as? LClass<*> ?: return emptyList()
            parentClazz.allProperties
        }
    }

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