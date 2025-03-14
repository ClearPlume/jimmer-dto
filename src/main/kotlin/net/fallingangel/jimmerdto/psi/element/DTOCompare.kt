package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOCompare : DTOElement {
    val prop: DTOQualifiedName

    val symbol: DTOCompareSymbol

    val value: DTOPropValue
}