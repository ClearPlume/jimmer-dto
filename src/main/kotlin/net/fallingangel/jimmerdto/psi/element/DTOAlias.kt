package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.jimmer.baseProperty
import net.fallingangel.jimmerdto.psi.grammarMismatch
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.parent

interface DTOAlias : DTONamedElement {
    val value: String

    val target: Resolution.Target?
        get() = when (val parent = parent) {
            is DTOImportStatement -> parent.qualifiedName.target
            is DTOImportedType -> parent.type.target
            else -> null
        }

    val hostName: String?
        get() = when (val parent = parent) {
            is DTOImportStatement -> parent.qualifiedName.value

            is DTOImportedType -> {
                val import = parent.parent<DTOImportStatement>() ?: grammarMismatch()
                val type = parent.type.value ?: return null
                "${import.qualifiedName.value}.$type"
            }

            is DTOPositiveProp -> parent.baseProperty?.name
            else -> grammarMismatch()
        }
}
