package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOImported : DTONamedElement {
    val value: String?

    val target: Resolution.Target?
        get() = value?.let { (parent as? DTOImportedType)?.target?.spaceForMembers()?.resolve(it) }
}
