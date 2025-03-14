package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImportedType : DTOElement {
    val type: String

    val alias: String?
}