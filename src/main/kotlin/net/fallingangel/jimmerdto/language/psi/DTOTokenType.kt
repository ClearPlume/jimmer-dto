package net.fallingangel.jimmerdto.language.psi

import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.language.DTOLanguage

class DTOTokenType(debugName: String) : IElementType(debugName, DTOLanguage.INSTANCE) {
    override fun toString() = "DTOTokenType.${super.toString()}"
}
