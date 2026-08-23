package net.fallingangel.jimmerdto.core

import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.DTOParser.*
import org.antlr.intellij.adaptor.parser.SyntaxError
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener
import org.antlr.v4.runtime.ParserRuleContext
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

        fun error(message: String?) {
            syntaxErrors += SyntaxError(recognizer, offendingSymbol, line, charPositionInLine, message, e)
        }

        val context = recognizer.context
        when {
            offendingSymbol.type in listOf(DTOLexer.StringLiteral, DTOLexer.SqlStringLiteral) && context.inside<AliasGroupContext>() -> {
                error("No quotation marks are needed here")
            }

            offendingSymbol.type == DTOLexer.Identifier && (context.inside<DefaultMorphismContext>() || context.inside<TypeMorphismContext>()) -> {
                error("To rename the generated class, use 'class ${offendingSymbol.text}' syntax")
            }

            offendingSymbol.type == DTOLexer.LBrace && context.inside<ClassDeclarationContext>() -> {
                error("Missing class name after 'class'")
            }

            offendingSymbol.type == DTOLexer.StringLiteral && context.inside<WhereArgsContext>() -> {
                error("String literals in SQL predicates must use single quotes")
            }

            else -> error(msg)
        }
    }

    private inline fun <reified T : ParserRuleContext> ParserRuleContext?.inside(): Boolean {
        return generateSequence(this) { it.parent as? ParserRuleContext }.any { it is T }
    }
}
