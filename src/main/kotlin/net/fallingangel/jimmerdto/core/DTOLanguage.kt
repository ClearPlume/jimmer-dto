package net.fallingangel.jimmerdto.core

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.xpath.XPath

const val LANGUAGE_NAME = "JimmerDTO"

object DTOLanguage : Language(LANGUAGE_NAME) {
    val xPath: XPath
        get() = XPath(DTOLanguage, "")

    val token: List<IElementType>
        get() = PSIElementTypeFactory.getTokenIElementTypes(DTOLanguage)

    val rule: List<IElementType>
        get() = PSIElementTypeFactory.getRuleIElementTypes(DTOLanguage)

    val softKeywords: Set<String>
        get() = setOf("like", "null", "desc", "asc")

    init {
        val vocab = DTOParser.VOCABULARY

        PSIElementTypeFactory.defineLanguageIElementTypes(
            DTOLanguage,
            Array(vocab.maxTokenType + 1) { vocab.getSymbolicName(it) ?: "<INVALID>" },
            DTOParser.ruleNames,
        )
    }

    fun tokenSet(vararg tokens: Int): TokenSet {
        return TokenSet.create(*tokens.map(token::get).toTypedArray())
    }

    fun ruleSet(vararg rules: Int): TokenSet {
        return TokenSet.create(*rules.map(rule::get).toTypedArray())
    }

    @Suppress("unused")
    // Serializable 对象必须实现 'readResolve' 
    private fun readResolve(): Any = DTOLanguage
}