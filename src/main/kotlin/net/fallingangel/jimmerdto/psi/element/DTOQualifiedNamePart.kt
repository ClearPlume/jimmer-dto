package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOQualifiedNamePart : DTONamedElement {
    val part: String
}