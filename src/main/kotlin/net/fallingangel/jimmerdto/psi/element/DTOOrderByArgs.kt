package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOOrderByArgs : DTOElement {
    val orderItems: List<DTOOrderItem>
}