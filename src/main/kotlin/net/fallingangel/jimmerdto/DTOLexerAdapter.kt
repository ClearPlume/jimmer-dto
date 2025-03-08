package net.fallingangel.jimmerdto

import net.fallingangel.jimmerdto.psi.DTOLexer
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor

class DTOLexerAdapter : ANTLRLexerAdaptor(DTOLanguage, DTOLexer(null))
