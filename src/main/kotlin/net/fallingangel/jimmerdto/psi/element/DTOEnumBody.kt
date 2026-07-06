package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.parent

interface DTOEnumBody : DTOElement {
    val mappings: List<DTOEnumMapping>

    val values: List<String>
        get() {
            val prop = parent.parent<DTOPositiveProp>()
            val propType = prop.property?.actualType as? LType.EnumType<*, *> ?: return listOf()
            return propType.values.keys.toList()
        }
}