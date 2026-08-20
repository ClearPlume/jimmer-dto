package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOClassSuffix : DTOElement {
    val classOperator: PsiElement

    val classToken: PsiElement?

    val unsupportedSuffix: PsiElement?

    val qualifiedName: DTOQualifiedName?
        get() = (parent as DTOAnnotationSingleValue).qualifiedName

    val target: Resolution.Target?
        get() = qualifiedName?.target
}
