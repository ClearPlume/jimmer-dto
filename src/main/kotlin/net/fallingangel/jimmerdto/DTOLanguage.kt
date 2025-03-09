package net.fallingangel.jimmerdto

import com.intellij.lang.Language
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory

object DTOLanguage : Language(Constant.NAME) {
    val preludes by lazy {
        val basicTypes = BasicType.types()
        val genericTypes = GenericType.types().map { it.presentation }
        basicTypes + genericTypes
    }

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