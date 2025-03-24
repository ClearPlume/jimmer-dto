package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.parent
import net.fallingangel.jimmerdto.util.propPath

interface DTOEnumBody : DTOElement {
    val mappings: List<DTOEnumMapping>

    val values: List<String>
        get() {
            val prop = parent.parent<DTOPositiveProp>()
            val propType = file.clazz.findPropertyOrNull(prop.propPath())?.actualType as? LType.EnumType<*, *> ?: return listOf()
            return propType.values.keys.toList()
        }
}