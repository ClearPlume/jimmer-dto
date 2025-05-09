package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotationValue : DTOElement {
    val singleValue: DTOAnnotationSingleValue?

    val arrayValue: DTOAnnotationArrayValue?

    val isEmpty: Boolean
        get() = singleValue == null && arrayValue == null
}