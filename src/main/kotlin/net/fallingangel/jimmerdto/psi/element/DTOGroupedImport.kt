package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOGroupedImport : DTOElement {
    val types: List<DTOImportedType>

    val target: Resolution.Target?
        get() = (parent as? DTOImportStatement)?.qualifiedName?.target
}