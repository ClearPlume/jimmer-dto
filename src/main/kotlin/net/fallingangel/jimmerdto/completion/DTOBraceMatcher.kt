package net.fallingangel.jimmerdto.completion

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOParser.*

class DTOBraceMatcher : PairedBraceMatcher {
    override fun getPairs() = arrayOf(
        BracePair(token[LBrace], token[RBrace], true),
        BracePair(token[LParen], token[RParen], true),
        BracePair(token[LBracket], token[RBracket], true),
        BracePair(token[LessThan], token[GreaterThan], true),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int) = openingBraceOffset
}
