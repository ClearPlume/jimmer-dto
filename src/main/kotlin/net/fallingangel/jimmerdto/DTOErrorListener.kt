package net.fallingangel.jimmerdto

import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.parser.SyntaxError
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token

class DTOErrorListener : SyntaxErrorListener() {
    private val syntaxErrors = mutableListOf<SyntaxError>()

    override fun getSyntaxErrors() = syntaxErrors

    /**
     * @param recognizer 报错发生时的 parser 实例；[DTOParser.context] 是当前所在的产生式规则上下文
     * @param offendingSymbol 触发报错的 token；[Token.type] 是 lexer token 类型，[Token.text] 是原文
     */
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        if (recognizer !is DTOParser || offendingSymbol !is Token) {
            return
        }
        when (recognizer.context) {
            is DTOParser.AliasGroupContext -> {
                if (offendingSymbol.type in listOf(DTOLexer.StringLiteral, DTOLexer.SqlStringLiteral)) {
                    syntaxErrors += SyntaxError(
                        recognizer,
                        offendingSymbol,
                        line,
                        charPositionInLine,
                        "No quotation marks are needed here",
                        e,
                    )
                }
            }

            is DTOParser.MorphismContext -> {
                if (offendingSymbol.type == DTOLexer.Identifier) {
                    syntaxErrors += SyntaxError(
                        recognizer,
                        offendingSymbol,
                        line,
                        charPositionInLine,
                        "To rename the generated class, use 'class ${offendingSymbol.text}' syntax",
                        e,
                    )
                }
            }

            is DTOParser.ClassDeclarationContext -> {
                if (offendingSymbol.type == DTOLexer.LBrace) {
                    syntaxErrors += SyntaxError(
                        recognizer,
                        offendingSymbol,
                        line,
                        charPositionInLine,
                        "Missing class name after 'class'",
                        e,
                    )
                }
            }

            else -> syntaxErrors += SyntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
        }
    }
}