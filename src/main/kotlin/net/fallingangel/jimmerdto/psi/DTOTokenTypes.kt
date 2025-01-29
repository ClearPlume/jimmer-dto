package net.fallingangel.jimmerdto.psi

import com.intellij.psi.tree.TokenSet

object DTOTokenTypes {
    @JvmField
    val LINE_COMMENT = DTOTokenType("LINE_COMMENT")

    @JvmField
    val BLOCK_COMMENT = DTOTokenType("BLOCK_COMMENT")

    @JvmField
    val DOC_COMMENT = DTOTokenType("DOC_COMMENT")

    val comments = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT, DOC_COMMENT)
}
