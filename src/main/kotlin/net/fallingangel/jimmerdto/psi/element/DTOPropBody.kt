package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationOwner
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropBody : DTOElement, DTOAnnotationOwner {
    val annotations: List<DTOAnnotation>

    val implements: DTOImplements?

    val dtoBody: DTODtoBody?

    val enumBody: DTOEnumBody?

    override val annotationSite: LAnnotationSite
        get() = LAnnotationSite.Type
}
