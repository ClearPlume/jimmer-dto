// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package net.fallingangel.jimmerdto.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import net.fallingangel.jimmerdto.language.psi.DTOTypes;
import net.fallingangel.jimmerdto.language.psi.DTOTokenTypes;

import java.util.Stack;

%%

%class DTOLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
IDENTIFIER = [$A-Za-z_][$\w]*
LINE_COMMENT = "//"[^\r\n]*
BLOCK_COMMENT = "/*"[*\r\s\.]*"*/"

MODIFIER = abstract | input | input-only | inputOnly | out | in
BOOLEAN = true | false
CHAR = '[^']'
STRING = \"[^\"]*\"
INTEGER = \d+
FLOAT = \d+\.\d+

%state BLOCK_COMMENT DOC_COMMENT

%%

<YYINITIAL> {
    {LINE_COMMENT}                                              { return DTOTokenTypes.LINE_COMMENT; }
    "/*"                                                        { yybegin(BLOCK_COMMENT); }
    "/**"                                                       { yybegin(DOC_COMMENT); }

    "import"                                                    { return DTOTypes.IMPORT_KEYWORD; }
    "as"                                                        { return DTOTypes.AS_KEYWORD; }
    {MODIFIER}                                                  { return DTOTypes.MODIFIER; }
    {BOOLEAN}                                                   { return DTOTypes.BOOLEAN_CONSTANT; }
    "null"                                                      { return DTOTypes.NULL_CONSTANT; }
    {CHAR}                                                      { return DTOTypes.CHAR_CONSTANT; }
    {STRING}                                                    { return DTOTypes.STRING_CONSTANT; }
    {INTEGER}                                                   { return DTOTypes.INTEGER_CONSTANT; }
    {FLOAT}                                                     { return DTOTypes.FLOAT_CONSTANT; }
    {IDENTIFIER}                                                { return DTOTypes.IDENTIFIER; }

    ","                                                         { return DTOTypes.COMMA; }
    "."                                                         { return DTOTypes.DOT; }
    "@"                                                         { return DTOTypes.AT; }
    "="                                                         { return DTOTypes.EQUALS; }
    ":"                                                         { return DTOTypes.COLON; }
    ";"                                                         { return DTOTypes.SEMICOLON; }
    "#"                                                         { return DTOTypes.HASH; }
    "+"                                                         { return DTOTypes.PLUS; }
    "-"                                                         { return DTOTypes.MINUS; }
    "?"                                                         { return DTOTypes.OPTIONAL; }
    "!"                                                         { return DTOTypes.REQUIRED; }
    "*"                                                         { return DTOTypes.ASTERISK; }
    "^"                                                         { return DTOTypes.POWER; }
    "$"                                                         { return DTOTypes.DOLLAR; }
    "->"                                                        { return DTOTypes.ARROW; }
    "("                                                         { return DTOTypes.PAREN_L; }
    ")"                                                         { return DTOTypes.PAREN_R; }
    "["                                                         { return DTOTypes.BRACKET_L; }
    "]"                                                         { return DTOTypes.BRACKET_R; }
    "<"                                                         { return DTOTypes.ANGLE_BRACKET_L; }
    ">"                                                         { return DTOTypes.ANGLE_BRACKET_R; }
    "{"                                                         { return DTOTypes.BRACE_L; }
    "}"                                                         { return DTOTypes.BRACE_R; }

    ({CRLF}|{WHITE_SPACE})+                                     { return TokenType.WHITE_SPACE; }

    [^]                                                         { return TokenType.BAD_CHARACTER; }
}

<BLOCK_COMMENT> {
    [^"*/"]                                                     {}
    [^\r\n]                                                     {}
    "*/"                                                        { yybegin(YYINITIAL); return DTOTokenTypes.BLOCK_COMMENT; }
}

<DOC_COMMENT> {
    [^"*/"]                                                     {}
    [^\r\n]                                                     {}
    "*/"                                                        { yybegin(YYINITIAL); return DTOTokenTypes.DOC_COMMENT; }
}
