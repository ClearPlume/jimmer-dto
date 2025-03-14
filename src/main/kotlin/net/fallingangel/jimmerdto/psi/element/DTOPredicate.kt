package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPredicate : DTOElement {
    val compare: DTOCompare?

    val nullity: DTONullity?
}