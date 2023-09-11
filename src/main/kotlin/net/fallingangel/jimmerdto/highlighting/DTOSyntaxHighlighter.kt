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
            DTOTokenTypes.LINE_COMMENT -> LINE_COMMENT_KEYS
            DTOTokenTypes.BLOCK_COMMENT -> BLOCK_COMMENT_KEYS
            DTOTokenTypes.DOC_COMMENT -> DOC_COMMENT_KEYS

            DTOTypes.AS_KEYWORD, DTOTypes.IMPORT_KEYWORD, DTOTypes.BOOLEAN_CONSTANT, DTOTypes.NULL_CONSTANT -> KEYWORD_KEYS
            DTOTypes.MODIFIER -> MODIFIER_KEYS
            DTOTypes.NEGATIVE_PROP -> NEGATIVE_PROP_KEYS

            DTOTypes.CHAR_CONSTANT -> CHAR_KEYS
            DTOTypes.STRING_CONSTANT -> STRING_KEYS
            DTOTypes.INTEGER_CONSTANT, DTOTypes.FLOAT_CONSTANT -> NUMBER_KEYS

            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
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
        val NEGATIVE_PROP = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NEGATIVE_PROP", CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
        val NAMED_PARAMETER_NAME = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_NAMED_PARAMETER_NAME")

        val ERROR = TextAttributesKey.createTextAttributesKey("JIMMER_DTO_ERROR")

        private val LINE_COMMENT_KEYS = arrayOf(LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(BLOCK_COMMENT)
        private val DOC_COMMENT_KEYS = arrayOf(DOC_COMMENT)

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val MODIFIER_KEYS = arrayOf(MODIFIER)
        private val NEGATIVE_PROP_KEYS = arrayOf(NEGATIVE_PROP)

        private val CHAR_KEYS = arrayOf(CHAR)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)

        private val BAD_CHAR_KEYS = arrayOf(HighlighterColors.BAD_CHARACTER)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }
}
