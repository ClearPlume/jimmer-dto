package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOParser.DoubleQuote
import net.fallingangel.jimmerdto.psi.DTOParser.SingleQuote

class DTOQuoteHandler : SimpleTokenSetQuoteHandler(token[SingleQuote], token[DoubleQuote])