package net.fallingangel.jimmerdto.psi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

fun DTOElement.grammarMismatch(): Nothing {
    error("No branch matched for ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}

fun PsiElement.unhandledElement(): Nothing {
    error("Unhandled element type ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}