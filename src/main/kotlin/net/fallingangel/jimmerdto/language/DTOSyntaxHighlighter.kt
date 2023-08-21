package net.fallingangel.jimmerdto.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.language.psi.DTOTypes


class DTOSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = DTOLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        if (tokenType == DTOTypes.SEPARATOR) {
            return SEPARATOR_KEYS
        }
        if (tokenType == DTOTypes.KEY) {
            return KEY_KEYS
        }
        if (tokenType == DTOTypes.VALUE) {
            return VALUE_KEYS
        }
        if (tokenType == DTOTypes.COMMENT) {
            return COMMENT_KEYS
        }
        if (tokenType == TokenType.BAD_CHARACTER) {
            return BAD_CHAR_KEYS
        }
        return EMPTY_KEYS
    }

    companion object {
        val SEPARATOR = TextAttributesKey.createTextAttributesKey("DTO_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val KEY = TextAttributesKey.createTextAttributesKey("DTO_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE = TextAttributesKey.createTextAttributesKey("DTO_VALUE", DefaultLanguageHighlighterColors.STRING)
        val COMMENT = TextAttributesKey.createTextAttributesKey("DTO_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("DTO_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val SEPARATOR_KEYS = arrayOf(SEPARATOR)
        private val KEY_KEYS = arrayOf(KEY)
        private val VALUE_KEYS = arrayOf(VALUE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = arrayOf<TextAttributesKey>()
    }
}
