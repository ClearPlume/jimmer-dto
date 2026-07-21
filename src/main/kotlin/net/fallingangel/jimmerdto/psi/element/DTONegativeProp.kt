package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTONegativeProp : DTOElement {
    val name: DTOPropName?

    // negativeProp -> dtoBody
    val containingLClass: LClass?
        get() = (parent as DTODtoBody).containingLClass
}