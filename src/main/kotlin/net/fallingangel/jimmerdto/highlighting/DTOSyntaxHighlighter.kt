package net.fallingangel.jimmerdto.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.DTOLexerAdapter
import net.fallingangel.jimmerdto.psi.DTOTokenTypes
import net.fallingangel.jimmerdto.psi.DTOTypes

class DTOSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = DTOLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            DTOTokenTypes.LINE_COMMENT -> arrayOf(LINE_COMMENT)
            DTOTokenTypes.BLOCK_COMMENT -> arrayOf(BLOCK_COMMENT)
            DTOTokenTypes.DOC_COMMENT -> arrayOf(DOC_COMMENT)

            DTOTypes.AS,
            DTOTypes.EXPORT,
            DTOTypes.PACKAGE,
            DTOTypes.IMPORT,
            DTOTypes.CLASS,
            DTOTypes.IMPLEMENTS,
            DTOTypes.BOOLEAN,
            DTOTypes.NULL,
            DTOTypes.IS,
            DTOTypes.NOT,
            DTOTypes.AND,
            DTOTypes.OR -> arrayOf(KEYWORD)

            DTOTypes.MODIFIER -> arrayOf(MODIFIER)
            DTOTypes.NEGATIVE_PROP -> arrayOf(NEGATIVE_PROP)

            DTOTypes.CHAR -> arrayOf(CHAR)
            DTOTypes.STRING, DTOTypes.SQL_STRING -> arrayOf(STRING)
            DTOTypes.INT, DTOTypes.FLOAT -> arrayOf(NUMBER)

            DTOTypes.WHERE_KEYWORD,
            DTOTypes.ORDER_BY_KEYWORD,
            DTOTypes.FILTER_KEYWORD,
            DTOTypes.RECURSION_KEYWORD,
            DTOTypes.FETCH_TYPE_KEYWORD,
            DTOTypes.LIMIT_KEYWORD,
            DTOTypes.OFFSET_KEYWORD,
            DTOTypes.BATCH_KEYWORD,
            DTOTypes.DEPTH_KEYWORD -> arrayOf(PROP_CONFIG)

            TokenType.BAD_CHARACTER -> arrayOf(HighlighterColors.BAD_CHARACTER)
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

        val NOT_USED = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NOT_USED", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
        val ERROR = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_ERROR")
        val DUPLICATION = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_DUPLICATION")
    }
}
