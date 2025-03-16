package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOGroupedImport : DTOElement {
    val types: List<DTOImportedType>
}