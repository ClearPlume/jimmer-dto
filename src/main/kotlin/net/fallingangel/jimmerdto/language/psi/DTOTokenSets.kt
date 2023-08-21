package net.fallingangel.jimmerdto.language.psi

import com.intellij.psi.tree.TokenSet

object DTOTokenSets {
    @Suppress("unused")
    val IDENTIFIERS = TokenSet.create(DTOTypes.KEY)
    val COMMENTS = TokenSet.create(DTOTypes.COMMENT)
}
