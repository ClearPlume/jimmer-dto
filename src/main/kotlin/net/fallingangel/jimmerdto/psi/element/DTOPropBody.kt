package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropBody : DTOElement {
    val annotations: List<DTOAnnotation>

    val implements: DTOImplements?

    val dtoBody: DTODtoBody?

    val enumBody: DTOEnumBody?
}