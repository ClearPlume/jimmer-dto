package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOMacroName : DTONamedElement {
    val value: String
}