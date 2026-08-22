package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationHost
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropBody : DTOElement, DTOAnnotationHost {
    val annotations: List<DTOAnnotation>

    val implements: DTOImplements?

    val dtoBody: DTODtoBody?

    val enumBody: DTOEnumBody?

    override val site: LAnnotationSite
        get() = LAnnotationSite.Type
}
