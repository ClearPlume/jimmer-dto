package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

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
}