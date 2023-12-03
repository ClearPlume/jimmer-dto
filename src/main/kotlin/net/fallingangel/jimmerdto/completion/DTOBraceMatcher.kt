package net.fallingangel.jimmerdto.completion

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.psi.DTOTypes

class DTOBraceMatcher : PairedBraceMatcher {
    override fun getPairs() = arrayOf(
        BracePair(DTOTypes.BRACE_L, DTOTypes.BRACE_R, true),
        BracePair(DTOTypes.PAREN_L, DTOTypes.PAREN_R, false),
        BracePair(DTOTypes.BRACKET_L, DTOTypes.BRACKET_R, false),
        BracePair(DTOTypes.ANGLE_BRACKET_L, DTOTypes.ANGLE_BRACKET_R, false)
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        println("getCodeConstructStart: $openingBraceOffset")
        println(file.text)
        return openingBraceOffset
    }
}
