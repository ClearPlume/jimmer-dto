package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImportStatement : DTOElement {
    val qualifiedName: DTOQualifiedName

    val alias: DTOAlias?

    val groupedImport: DTOGroupedImport?

    /**
     * [groupedImport] 非空时不可使用
     */
    val simpleName: String
        get() = alias?.value ?: qualifiedName.simpleName
}