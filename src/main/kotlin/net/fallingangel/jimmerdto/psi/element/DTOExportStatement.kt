package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOExportStatement : DTOElement {
    val export: DTOQualifiedName

    val `package`: DTOQualifiedName?
}