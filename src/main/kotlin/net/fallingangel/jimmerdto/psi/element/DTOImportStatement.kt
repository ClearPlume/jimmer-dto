package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImportStatement : DTOElement {
    val qualifiedName: DTOQualifiedName

    val alias: DTOAlias?

    val groupedImport: DTOGroupedImport?
}