package net.fallingangel.jimmerdto

import com.intellij.lang.Language
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.xpath.XPath

object DTOLanguage : Language(Constant.NAME) {
    val xPath by lazy { XPath(DTOLanguage, "") }

    val preludes by lazy {
        val basicTypes = BasicType.types()
        val genericTypes = GenericType.types().map { it.presentation }
        basicTypes + genericTypes
    }

    val availableFetchTypes by lazy { listOf("SELECT", "JOIN_IF_NO_CACHE", "JOIN_ALWAYS") }

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