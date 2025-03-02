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
WhiteSpace=[\ \n\t\f]
Identifier = [A-Za-z_$][$\w]*

LineComment = "//"[^\r\n]*
CommentContent = ([^*] | \*+ [^/*])*
BlockComment = "/*"{CommentContent}"*/"
DocComment = "/**"{CommentContent}"*/"

Modifier = input | specification | abstract | unsafe | dynamic | fixed | static | fuzzy | out | in
Boolean = true | false
Char = '([^'\\]|\\[btnfr'\\])'
String = \"([^\"\\]|\\.)*\"
SqlString = '([^']|'')*'

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
ExponentFloating = {SignedInteger}(\.{Digits})?{ExponentPart}{FloatTypeSuffix}?
// 小数
DecimalFloatingPoint = {SignedInteger}\.{Digits}{FloatTypeSuffix}?
// 十六进制小数『0xAB.Cp10』
HexFloatingPoint = 0[xX]{HexDigit}+\.{HexDigit}+[pP]{SignedInteger}{FloatTypeSuffix}?
FloatingPoint = {ExponentFloating}|{DecimalFloatingPoint}|{HexFloatingPoint}

%%

<YYINITIAL> {
    {DocComment}                                            { return DTOTokenTypes.DOC_COMMENT; }
    {BlockComment}                                          { return DTOTokenTypes.BLOCK_COMMENT; }
    {LineComment}                                           { return DTOTokenTypes.LINE_COMMENT; }

    "export"                                                { return DTOTypes.EXPORT; }
    "package"                                               { return DTOTypes.PACKAGE; }
    "import"                                                { return DTOTypes.IMPORT; }
    "as"                                                    { return DTOTypes.AS; }
    "class"                                                 { return DTOTypes.CLASS; }
    "implements"                                            { return DTOTypes.IMPLEMENTS; }
    "!" {Identifier}                                        { return DTOTypes.PROP_CONFIG_NAME; }
    "and"                                                   { return DTOTypes.AND; }
    "or"                                                    { return DTOTypes.OR; }
    "is"                                                    { return DTOTypes.IS; }
    "not"                                                   { return DTOTypes.NOT; }
    "asc"                                                   { return DTOTypes.ASC; }
    "desc"                                                  { return DTOTypes.DESC; }
    {Modifier}                                              {
        if (braceLevel > 0) {
            return DTOTypes.IDENTIFIER;
        } else {
            return DTOTypes.MODIFIER;
        }
    }
    {Boolean}                                               { return DTOTypes.BOOLEAN; }
    "null"                                                  { return DTOTypes.NULL; }
    {Char}                                                  { return DTOTypes.CHAR; }
    {String}                                                { return DTOTypes.STRING; }
    {SqlString}                                             { return DTOTypes.SQL_STRING; }
    {Integer}                                               { return DTOTypes.INT; }
    {FloatingPoint}                                         { return DTOTypes.FLOAT; }

    ","                                                     { return DTOTypes.COMMA; }
    "."                                                     { return DTOTypes.DOT; }
    "@"                                                     { return DTOTypes.AT; }
    "="                                                     { return DTOTypes.EQ; }
    "<>"                                                    { return DTOTypes.UE; }
    "!="                                                    { return DTOTypes.NE; }
    "<"                                                     { return DTOTypes.LT; }
    "<="                                                    { return DTOTypes.LE; }
    ">"                                                     { return DTOTypes.GT; }
    ">="                                                    { return DTOTypes.GE; }
    "like"                                                  { return DTOTypes.LIKE; }
    "ilike"                                                 { return DTOTypes.ILIKE; }
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

    {Identifier}                                            { return DTOTypes.IDENTIFIER; }
    ({CRLF}|{WhiteSpace})+                                  { return TokenType.WHITE_SPACE; }

    [^]                                                     { return TokenType.BAD_CHARACTER; }
}