package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOOrderItem : DTOElement {
    val prop: DTOQualifiedName

    val direction: DTOOrderDirection?
}