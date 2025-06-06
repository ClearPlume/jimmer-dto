package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOTypeRef : DTOElement {
    val type: DTOQualifiedName

    val arguments: DTOGenericArguments?

    val questionMark: PsiElement?
}