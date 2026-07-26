package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropConfig : DTOElement {
    val name: PsiElement

    val whereArgs: DTOWhereArgs?

    val orderByArgs: DTOOrderByArgs?

    val qualifiedName: DTOQualifiedName?

    val intPair: DTOIntPair?

    val containingLClass: LClass?
        get() = (parent as? DTOPositiveProp)?.property?.targetClass
}