package net.fallingangel.jimmerdto.lsi

import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotationBySimple

interface LNullableAware : LAnnotationOwner {
    val type: LType

    val hasNullableAnnotation: Boolean
        get() = hasAnnotationBySimple("Null", "Nullable")

    val nullable: Boolean
        get() = hasNullableAnnotation || type.nullable

    val presentableType: String
        get() = buildString {
            append(type.presentableName)
            if (hasNullableAnnotation) {
                append("?")
            }
        }
}