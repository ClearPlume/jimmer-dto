package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOQualifiedNamePart : DTOElement {
    val part: String
}