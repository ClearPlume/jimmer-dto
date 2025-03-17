package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOImported : DTONamedElement {
    val value: String
}