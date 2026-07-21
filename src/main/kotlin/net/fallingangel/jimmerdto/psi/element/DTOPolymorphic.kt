package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPolymorphic : DTOElement {
    val directive: DTODirective

    val macros: List<DTOMacro>

    val morphisms: List<DTOMorphism>

    // polymorphic -> dtoBody
    val containingLClass: LClass?
        get() = (parent as DTODtoBody).containingLClass
}