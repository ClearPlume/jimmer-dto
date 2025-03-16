package net.fallingangel.jimmerdto

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType
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

    private fun readResolve(): Any = DTOLanguage
}