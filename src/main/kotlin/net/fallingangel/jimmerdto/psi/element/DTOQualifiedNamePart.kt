package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import org.jetbrains.kotlin.psi.psiUtil.prevSiblingOfSameType

interface DTOQualifiedNamePart : DTONamedElement {
    val part: String

    val prevPart: DTOQualifiedNamePart?
        get() = prevSiblingOfSameType()

    val qualifiedName: DTOQualifiedName
        get() = parent as DTOQualifiedName

    val space: Resolution.Space?
        get() {
            val predecessor = prevPart ?: return qualifiedName.initialSpace
            return predecessor.target?.spaceForMembers()
        }

    val target: Resolution.Target?
        get() = space?.resolve(part)
}