package net.fallingangel.jimmerdto.psi

import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.DTOLanguage

class DTOTokenType(debugName: String) : IElementType(debugName, DTOLanguage) {
    override fun toString() = "DTOTokenType.${super.toString()}"
}
