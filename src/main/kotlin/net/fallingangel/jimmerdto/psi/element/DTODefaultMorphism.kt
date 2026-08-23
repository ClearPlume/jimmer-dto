package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationHost

interface DTODefaultMorphism : DTOMorphism, DTOAnnotationHost {
    val default: PsiElement

    override val resolvedLClass: LClass?
        get() = containingLClass
}
