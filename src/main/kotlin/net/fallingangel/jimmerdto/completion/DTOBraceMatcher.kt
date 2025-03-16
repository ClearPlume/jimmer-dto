package net.fallingangel.jimmerdto.completion

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOLexer

class DTOBraceMatcher : PairedBraceMatcher {
    override fun getPairs() = arrayOf(
        BracePair(token[DTOLexer.LBrace], token[DTOLexer.RBrace], true),
        BracePair(token[DTOLexer.LParen], token[DTOLexer.RParen], true),
        BracePair(token[DTOLexer.LBracket], token[DTOLexer.RBracket], true),
        BracePair(token[DTOLexer.LessThan], token[DTOLexer.GreaterThan], true),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int) = openingBraceOffset
}
