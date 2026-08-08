package net.fallingangel.jimmerdto.editor

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import net.fallingangel.jimmerdto.core.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOParser.DoubleQuote
import net.fallingangel.jimmerdto.psi.DTOParser.SingleQuote

class DTOQuoteHandler : SimpleTokenSetQuoteHandler(token[SingleQuote], token[DoubleQuote])