package net.fallingangel.jimmerdto.language.psi

import com.intellij.psi.tree.TokenSet

object DTOTokenSets {
    val COMMENTS = TokenSet.create(DTOTypes.LINE_COMMENT, DTOTypes.BLOCK_COMMENT)
}
