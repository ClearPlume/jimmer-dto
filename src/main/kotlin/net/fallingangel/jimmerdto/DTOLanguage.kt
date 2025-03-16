package net.fallingangel.jimmerdto

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.xpath.XPath

object DTOLanguage : Language(Constant.NAME) {
    val xPath: XPath
        get() = XPath(DTOLanguage, "")

    val token: List<IElementType>
        get() = PSIElementTypeFactory.getTokenIElementTypes(DTOLanguage)

    val rule: List<IElementType>
        get() = PSIElementTypeFactory.getRuleIElementTypes(DTOLanguage)

    val preludes: List<String>
        get() {
            val basicTypes = BasicType.types()
            val genericTypes = GenericType.types().map { it.presentation }
            return basicTypes + genericTypes
        }

    val availableFetchTypes: List<String>
        get() = listOf("SELECT", "JOIN_IF_NO_CACHE", "JOIN_ALWAYS")

    init {
        val vocab = DTOParser.VOCABULARY

        PSIElementTypeFactory.defineLanguageIElementTypes(
            DTOLanguage,
            Array(vocab.maxTokenType + 1) { vocab.getDisplayName(it) },
            DTOParser.ruleNames,
        )
    }

    fun tokenSet(vararg tokens: Int): TokenSet {
        return TokenSet.create(*tokens.map(token::get).toTypedArray())
    }

    fun ruleSet(vararg rules: Int): TokenSet {
        return TokenSet.create(*rules.map(rule::get).toTypedArray())
    }

    private fun readResolve(): Any = DTOLanguage
}