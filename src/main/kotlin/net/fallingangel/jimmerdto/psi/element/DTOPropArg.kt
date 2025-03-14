package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropArg : DTOElement {
    val values: List<DTOValue>
}