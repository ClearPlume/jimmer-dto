package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOUserProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val name: DTOPropName

    val type: DTOTypeDef
}