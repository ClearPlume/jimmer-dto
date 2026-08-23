package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationHost
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOMorphism : DTOElement, DTOAnnotationHost {
    val annotations: List<DTOAnnotation>

    val classDeclaration: DTOClassDeclaration?

    val implements: DTOImplements?

    val dtoBody: DTODtoBody

    override val site: LAnnotationSite
        get() = LAnnotationSite.Type

    // morphism -> polymorphic
    val containingLClass: LClass?
        get() = (parent as DTOPolymorphic).containingLClass

    val resolvedLClass: LClass?
}
