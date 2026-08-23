package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationHost
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOTypeMorphism : DTOMorphism, DTOAnnotationHost {
    val targetType: DTOQualifiedName

    override val resolvedLClass: LClass?
        get() = when (val target = targetType.target) {
            is Resolution.Target.Subtype -> target.lClass
            is Resolution.Target.Type -> process(target.type) { lClass() }
            else -> null
        }
}
