package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOPropName : DTONamedElement {
    val value: String

    //         / positiveProp
    // propName - negativeProp
    //         \ userProp
    val containingLClass: LClass?
        get() = when (val parent = parent) {
            is DTOPositiveProp -> parent.containingLClass
            is DTONegativeProp -> parent.containingLClass
            is DTOUserProp -> parent.containingLClass
            else -> error("Unexpected parent: ${parent::class}")
        }
}