package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOEnumBody : DTOElement {
    val mappings: List<DTOEnumMapping>
}