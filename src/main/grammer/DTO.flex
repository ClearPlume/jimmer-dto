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
CHAR = '([^'\\]|\\[btnfr'\\])'
STRING = \"([^\"\\]|\\.)*\"
SQL_STRING = '([^']|'')*'

IntegerTypeSuffix = [lL]

DecimalNumeral = 0|[1-9][\d_]*
DecimalInteger = {DecimalNumeral}{IntegerTypeSuffix}?

HexDigit = [\da-fA-F_]
HexInteger = 0[xX]{HexDigit}+{IntegerTypeSuffix}?

OctalInteger = 0[0-7_]+{IntegerTypeSuffix}?

BinaryInteger = 0[bB][01_]+{IntegerTypeSuffix}?

Integer = {DecimalInteger}|{HexInteger}|{OctalInteger}|{BinaryInteger}

Digits = \d[\d_]*
SignedInteger = [+-]?{Digits}
ExponentPart = [eE]{SignedInteger}
FloatTypeSuffix = [FfDd]

// 指数『20e2』
ExponentFloating = {SignedInteger}\.{ExponentPart}{FloatTypeSuffix}?
// 小数
DecimalFloatingPoint = {SignedInteger}\.{Digits}{FloatTypeSuffix}?
// 十六进制小数『0xAB.Cp10』
HexFloatingPoint = 0[xX]{HexDigit}+\.{HexDigit}+[pP]{SignedInteger}{FloatTypeSuffix}?
FloatingPoint = {ExponentFloating}|{DecimalFloatingPoint}|{HexFloatingPoint}

%%

<YYINITIAL> {
    {DOC_COMMENT}                                           { return DTOTokenTypes.DOC_COMMENT; }
    {BLOCK_COMMENT}                                         { return DTOTokenTypes.BLOCK_COMMENT; }
    {LINE_COMMENT}                                          { return DTOTokenTypes.LINE_COMMENT; }

    "export"                                                { return DTOTypes.EXPORT; }
    "package"                                               { return DTOTypes.PACKAGE; }
    "import"                                                { return DTOTypes.IMPORT; }
    "as"                                                    { return DTOTypes.AS; }
    "class"                                                 { return DTOTypes.CLASS; }
    "implements"                                            { return DTOTypes.IMPLEMENTS; }
    "!where"                                                { return DTOTypes.WHERE_KEYWORD; }
    "and"                                                   { return DTOTypes.AND; }
    "or"                                                    { return DTOTypes.OR; }
    "is"                                                    { return DTOTypes.IS; }
    "not"                                                   { return DTOTypes.NOT; }
    "!orderBy"                                              { return DTOTypes.ORDER_BY_KEYWORD; }
    "asc"                                                   { return DTOTypes.ASC; }
    "desc"                                                  { return DTOTypes.DESC; }
    "!filter"                                               { return DTOTypes.FILTER_KEYWORD; }
    "!recursion"                                            { return DTOTypes.RECURSION_KEYWORD; }
    "!fetchType"                                            { return DTOTypes.FETCH_TYPE_KEYWORD; }
    "!limit"                                                { return DTOTypes.LIMIT_KEYWORD; }
    "!offset"                                               { return DTOTypes.OFFSET_KEYWORD; }
    "!batch"                                                { return DTOTypes.BATCH_KEYWORD; }
    "!depth"                                                { return DTOTypes.DEPTH_KEYWORD; }
    {MODIFIER}                                              {
        if (braceLevel > 0) {
            return DTOTypes.IDENTIFIER;
        } else {
            return DTOTypes.MODIFIER;
        }
    }
    {BOOLEAN}                                               { return DTOTypes.BOOLEAN_CONSTANT; }
    "null"                                                  { return DTOTypes.NULL; }
    {CHAR}                                                  { return DTOTypes.CHAR_CONSTANT; }
    {STRING}                                                { return DTOTypes.STRING_CONSTANT; }
    {SQL_STRING}                                            { return DTOTypes.SQL_STRING_CONSTANT; }
    {Integer}                                               { return DTOTypes.INTEGER_CONSTANT; }
    {FloatingPoint}                                         { return DTOTypes.FLOAT_CONSTANT; }

    ","                                                     { return DTOTypes.COMMA; }
    "."                                                     { return DTOTypes.DOT; }
    "@"                                                     { return DTOTypes.AT; }
    "="                                                     { return DTOTypes.EQ; }
    "<>"                                                    { return DTOTypes.NE; }
    "<"                                                     { return DTOTypes.LT; }
    "<="                                                    { return DTOTypes.LE; }
    ">"                                                     { return DTOTypes.GT; }
    ">="                                                    { return DTOTypes.GE; }
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
    "{"                                                     { braceLevel++; return DTOTypes.BRACE_L; }
    "}"                                                     { braceLevel--; return DTOTypes.BRACE_R; }

    {IDENTIFIER}                                            { return DTOTypes.IDENTIFIER; }
    ({CRLF}|{WHITE_SPACE})+                                 { return TokenType.WHITE_SPACE; }

    [^]                                                     { return TokenType.BAD_CHARACTER; }
}