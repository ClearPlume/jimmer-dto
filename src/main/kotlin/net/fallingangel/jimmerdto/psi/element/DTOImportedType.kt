package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImportedType : DTOElement {
    val type: DTOImported

    val alias: DTOAlias?
}