package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOAlias : DTONamedElement {
    val value: String
}