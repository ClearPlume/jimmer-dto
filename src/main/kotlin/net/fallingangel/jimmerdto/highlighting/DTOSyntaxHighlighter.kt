package net.fallingangel.jimmerdto.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOLexer
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor
import org.antlr.intellij.adaptor.lexer.TokenIElementType

class DTOSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = ANTLRLexerAdaptor(DTOLanguage, DTOLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        if (tokenType !is TokenIElementType) {
            return emptyArray()
        }
        return when (tokenType.antlrTokenType) {
            DTOLexer.LineComment -> arrayOf(LINE_COMMENT)
            DTOLexer.BlockComment -> arrayOf(BLOCK_COMMENT)
            DTOLexer.DocComment -> arrayOf(DOC_COMMENT)

            DTOLexer.As,
            DTOLexer.Export,
            DTOLexer.Package,
            DTOLexer.Import,
            DTOLexer.Class,
            DTOLexer.Implements,
            DTOLexer.BooleanLiteral,
            DTOLexer.Null,
            DTOLexer.Is,
            DTOLexer.Not,
            DTOLexer.And,
            DTOLexer.Like,
            DTOLexer.Ilike,
            DTOLexer.Or,
            DTOLexer.Asc,
            DTOLexer.Desc -> arrayOf(KEYWORD)

            DTOLexer.Modifier -> arrayOf(MODIFIER)

            DTOLexer.CharacterLiteral -> arrayOf(CHAR)
            DTOLexer.StringLiteral, DTOLexer.SqlStringLiteral -> arrayOf(STRING)
            DTOLexer.IntegerLiteral, DTOLexer.FloatingPointLiteral -> arrayOf(NUMBER)

            else -> arrayOf()
        }
    }

    companion object {
        val LINE_COMMENT = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val DOC_COMMENT = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)

        val KEYWORD = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val MODIFIER = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_MODIFIER", DefaultLanguageHighlighterColors.KEYWORD)

        val CHAR = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_CHAR", DefaultLanguageHighlighterColors.STRING)
        val STRING = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

        val ANNOTATION = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)
        val FUNCTION = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_FUNCTION", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val MACRO = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_MACRO", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val PROP_CONFIG = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_PROP_CONFIG")
        val NEGATIVE_PROP = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NEGATIVE_PROP", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
        val NAMED_PARAMETER_NAME = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NAMED_PARAMETER_NAME")
        val ENUM_INSTANCE = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_ENUM_INSTANCE", DefaultLanguageHighlighterColors.STATIC_FIELD)
        val VALUE = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_VALUE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

        val ERROR = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_ERROR")
        val DUPLICATION = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_DUPLICATION")
    }
}
