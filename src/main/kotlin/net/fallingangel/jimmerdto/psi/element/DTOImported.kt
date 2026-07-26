package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOImported : DTONamedElement {
    val value: String

    val target: Resolution.Target?
        get() = (parent as? DTOImportedType)?.target?.spaceForMembers()?.resolve(value)
}