package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTONegativeProp : DTOElement {
    val name: DTOPropName?

    // negativeProp -> dtoBody
    val containingLClass: LClass?
        get() = (parent as DTODtoBody).containingLClass

    val property: LProperty?
        get() = name?.value?.let { containingLClass?.findProperty(it) }
}
