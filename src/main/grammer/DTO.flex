// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package net.fallingangel.jimmerdto;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import net.fallingangel.jimmerdto.psi.DTOTypes;
import net.fallingangel.jimmerdto.psi.DTOTokenTypes;

%%

%class DTOLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
    int braceLevel = 0;
%}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
IDENTIFIER = [A-Za-z_$][$\w]*

LINE_COMMENT = "//"[^\r\n]*
COMMENT_CONTENT = ([^*] | \*+ [^/*])*
BLOCK_COMMENT = "/*"{COMMENT_CONTENT}"*/"
DOC_COMMENT = "/**"{COMMENT_CONTENT}"*/"

MODIFIER = input | specification | abstract | unsafe | dynamic | fixed | static | fuzzy | out | in
BOOLEAN = true | false
CHAR = '[^']'
STRING = \"[^\"]*\"
INTEGER = \d+
FLOAT = \d+\.\d+

%s PROP_FUNCTION

%%

<YYINITIAL> {
    {DOC_COMMENT}                                           { return DTOTokenTypes.DOC_COMMENT; }
    {BLOCK_COMMENT}                                         { return DTOTokenTypes.BLOCK_COMMENT; }
    {LINE_COMMENT}                                          { return DTOTokenTypes.LINE_COMMENT; }

    "null" " "* "("                                         { yypushback(yylength()); yybegin(PROP_FUNCTION); }

    "export"                                                { return DTOTypes.EXPORT_KEYWORD; }
    "package"                                               { return DTOTypes.PACKAGE_KEYWORD; }
    "import"                                                { return DTOTypes.IMPORT_KEYWORD; }
    "as"                                                    { return DTOTypes.AS_KEYWORD; }
    "this"                                                  { return DTOTypes.THIS_KEYWORD; }
    "class"                                                 { return DTOTypes.CLASS_KEYWORD; }
    "implements"                                            { return DTOTypes.IMPLEMENTS_KEYWORD; }
    {MODIFIER}                                              {
        if (braceLevel > 0) {
            return DTOTypes.IDENTIFIER;
        } else {
            return DTOTypes.MODIFIER;
        }
    }
    {BOOLEAN}                                               { return DTOTypes.BOOLEAN_CONSTANT; }
    "null"                                                  { return DTOTypes.NULL_CONSTANT; }
    {CHAR}                                                  { return DTOTypes.CHAR_CONSTANT; }
    {STRING}                                                { return DTOTypes.STRING_CONSTANT; }
    {INTEGER}                                               { return DTOTypes.INTEGER_CONSTANT; }
    {FLOAT}                                                 { return DTOTypes.FLOAT_CONSTANT; }

    ","                                                     { return DTOTypes.COMMA; }
    "."                                                     { return DTOTypes.DOT; }
    "@"                                                     { return DTOTypes.AT; }
    "="                                                     { return DTOTypes.EQUALS; }
    ":"                                                     { return DTOTypes.COLON; }
    ";"                                                     { return DTOTypes.SEMICOLON; }
    "#"                                                     { return DTOTypes.HASH; }
    "+"                                                     { return DTOTypes.PLUS; }
    "-"                                                     { return DTOTypes.MINUS; }
    "?"                                                     { return DTOTypes.OPTIONAL; }
    "!"                                                     { return DTOTypes.REQUIRED; }
    "*"                                                     { return DTOTypes.ASTERISK; }
    "^"                                                     { return DTOTypes.POWER; }
    "$"                                                     { return DTOTypes.DOLLAR; }
    "->"                                                    { return DTOTypes.ARROW; }
    "/"                                                     { return DTOTypes.SLASH; }
    "::"                                                    { return DTOTypes.CLASS_REFERENCE; }
    "("                                                     { return DTOTypes.PAREN_L; }
    ")"                                                     { return DTOTypes.PAREN_R; }
    "["                                                     { return DTOTypes.BRACKET_L; }
    "]"                                                     { return DTOTypes.BRACKET_R; }
    "<"                                                     { return DTOTypes.ANGLE_BRACKET_L; }
    ">"                                                     { return DTOTypes.ANGLE_BRACKET_R; }
    "{"                                                     { braceLevel++; return DTOTypes.BRACE_L; }
    "}"                                                     { braceLevel--; return DTOTypes.BRACE_R; }

    {IDENTIFIER}                                            { return DTOTypes.IDENTIFIER; }
    ({CRLF}|{WHITE_SPACE})+                                 { return TokenType.WHITE_SPACE; }

    [^]                                                     { return TokenType.BAD_CHARACTER; }
}

<PROP_FUNCTION> {
    "null"                           { return DTOTypes.IDENTIFIER; }
    {IDENTIFIER}                     { return DTOTypes.IDENTIFIER; }
    "("                              { return DTOTypes.PAREN_L; }
    ")"                              { yybegin(YYINITIAL); return DTOTypes.PAREN_R; }
}