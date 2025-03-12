package net.fallingangel.jimmerdto

import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.parser.SyntaxError
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token

class DTOErrorListener : SyntaxErrorListener() {
    private val syntaxErrors = mutableListOf<SyntaxError>()

    override fun getSyntaxErrors() = syntaxErrors

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
            else -> syntaxErrors += SyntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
        }
    }
}