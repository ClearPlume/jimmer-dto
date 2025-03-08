package net.fallingangel.jimmerdto

import com.intellij.lang.Language
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory

object DTOLanguage : Language(Constant.NAME) {
    init {
        val vocab = DTOParser.VOCABULARY

        PSIElementTypeFactory.defineLanguageIElementTypes(
            DTOLanguage,
            Array(vocab.maxTokenType + 1) { vocab.getDisplayName(it) },
            DTOParser.ruleNames,
        )
    }

    private fun readResolve(): Any = DTOLanguage
}