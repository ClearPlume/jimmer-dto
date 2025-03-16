package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOGenericArguments : DTOElement {
    val values: List<DTOGenericArgument>
}