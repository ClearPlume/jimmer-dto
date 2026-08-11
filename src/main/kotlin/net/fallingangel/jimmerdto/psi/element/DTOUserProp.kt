package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOUserProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val name: DTOPropName

    val type: DTOTypeRef

    val equals: PsiElement?

    val defaultValue: DTODefaultValue?

    // userProp - dtoBody
    val containingLClass: LClass?
        get() = (parent as DTODtoBody).containingLClass
}