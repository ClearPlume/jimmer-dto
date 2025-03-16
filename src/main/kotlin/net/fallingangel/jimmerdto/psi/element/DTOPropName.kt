package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOPropName : DTONamedElement {
    val value: String
}