package net.fallingangel.jimmerdto.psi

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

fun DTOElement.grammarMismatch(): Nothing {
    error("No branch matched for ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}
