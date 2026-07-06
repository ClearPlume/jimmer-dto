package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOValue : DTONamedElement {
    // value -> propArg -> positiveProp
    val containingLClass: LClass<*>?
        get() = (parent.parent as DTOPositiveProp).containingLClass

    val property: LProperty<*>?
        get() = containingLClass?.findProperty(text)

    val resolvedLClass: LClass<*>?
        get() = property?.actualType as? LClass<*>
}