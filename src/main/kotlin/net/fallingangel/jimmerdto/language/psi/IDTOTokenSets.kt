package net.fallingangel.jimmerdto.language.psi

import com.intellij.psi.tree.TokenSet

interface IDTOTokenSets {
    companion object {
    val IDENTIFIERS = TokenSet.create(DTOTypes.KEY)
    val COMMENTS = TokenSet.create(DTOTypes.COMMENT)
    }
}
