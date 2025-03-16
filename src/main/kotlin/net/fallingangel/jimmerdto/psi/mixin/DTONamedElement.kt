package net.fallingangel.jimmerdto.psi.mixin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface DTONamedElement : DTOElement, PsiNameIdentifierOwner {
    override fun setName(name: String) = this

    fun resolve(): PsiElement? = null
}