package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOUserProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val name: DTOPropName

    val type: DTOTypeRef

    // userProp - dtoBody
    val containingLClass: LClass<*>?
        get() = (parent as DTODtoBody).containingLClass
}