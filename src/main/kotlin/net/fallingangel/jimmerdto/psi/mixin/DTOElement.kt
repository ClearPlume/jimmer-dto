package net.fallingangel.jimmerdto.psi.mixin

import com.intellij.psi.PsiElement

interface DTOElement : PsiElement {
    fun grammarMismatch(): Nothing {
        error("No branch matched for ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
    }
}