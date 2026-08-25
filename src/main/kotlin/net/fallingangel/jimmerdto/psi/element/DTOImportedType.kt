package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOImportedType : DTOElement {
    val type: DTOImported

    val alias: DTOAlias?

    val target: Resolution.Target?
        get() = (parent as? DTOGroupedImport)?.target

    val simpleName: String?
        get() = alias?.value ?: type.value
}
