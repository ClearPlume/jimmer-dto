package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotationArrayValue : DTOElement {
    val values: List<DTOAnnotationValue>
}