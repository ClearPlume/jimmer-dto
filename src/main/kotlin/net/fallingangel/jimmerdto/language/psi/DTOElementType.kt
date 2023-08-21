package net.fallingangel.jimmerdto.language.psi

import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.language.DTOLanguage

class DTOElementType(debugName: String) : IElementType(debugName, DTOLanguage.INSTANCE)
