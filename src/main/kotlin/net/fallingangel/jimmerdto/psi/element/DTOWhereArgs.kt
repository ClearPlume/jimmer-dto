package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOWhereArgs : DTOElement {
    val predicates: List<DTOPredicate>
}