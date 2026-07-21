package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.parent

interface DTOEnumBody : DTOElement {
    val mappings: List<DTOEnumMapping>

    val values: List<String>
        get() {
            val prop = parent.parent<DTOPositiveProp>()
            val propType = prop.property?.actualType as? LProperty.Type.Enum ?: return listOf()
            return propType.constants.keys.toList()
        }
}