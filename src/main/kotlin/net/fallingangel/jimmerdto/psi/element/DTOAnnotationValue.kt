package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotationValue : DTOElement {
    val singleValue: DTOAnnotationSingleValue?

    val arrayValue: DTOAnnotationArrayValue?

    val value: LAnnotation.Param.Value?
        get() {
            val arrayValue = arrayValue
            val singleValue = singleValue

            return when {
                arrayValue != null -> LAnnotation.Param.Value.Array(arrayValue.values.map { it.value })
                singleValue != null -> singleValue.value
                else -> error("DTOAnnotationValue without singleValue or arrayValue in ${containingFile.name}")
            }
        }
}