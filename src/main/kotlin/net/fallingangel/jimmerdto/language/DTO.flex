// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package net.fallingangel.jimmerdto.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import net.fallingangel.jimmerdto.language.psi.DTOTypes;

import java.util.Stack;

%%

%{
private static final class State {
    final int state;

    public State(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "yystate = " + state;
    }
}

protected final Stack<State> stateStack = new Stack<State>();

private void pushState(int state) {
    stateStack.push(new State(yystate()));
    yybegin(state);
}

private void popState() {
    State state = stateStack.pop();
    yybegin(state.state);
}
%}

%class DTOLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
FIRST_VALUE_CHARACTER=[^ \n\f\\] | "\\"{CRLF} | "\\".
VALUE_CHARACTER=[^\n\f\\] | "\\"{CRLF} | "\\".
END_OF_LINE_COMMENT=("#"|"!")[^\r\n]*
SEPARATOR=[:=]
KEY_CHARACTER=[^:=\ \n\t\f\\] | "\\ "

IDENTIFIER = [$A-Za-z_][$\w]*
LINE_COMMENT = "//"[^\r\n]*
BLOCK_COMMENT = "/*"([^*]* | [*]*)"*/"

MODIFIER = abstract | input | input-only | inputOnly
BOOLEAN = true | false
CHAR = '[^']'
STRING = \"[^\"]*\"
INTEGER = \d+
FLOAT = \d+\.\d+

%state ANNOTATION

%%

<YYINITIAL> {
    "import"                                                    { return DTOTypes.IMPORT_KEYWORD; }
    "as"                                                        { return DTOTypes.AS_KEYWORD; }

    {MODIFIER}                                                  { return DTOTypes.MODIFIER; }
    {BOOLEAN}                                                   { return DTOTypes.BOOLEAN_CONSTANT; }
    "null"                                                      { return DTOTypes.NULL_CONSTANT; }
    {CHAR}                                                      { return DTOTypes.CHAR_CONSTANT; }
    {STRING}                                                    { return DTOTypes.STRING_CONSTANT; }
    {INTEGER}                                                   { return DTOTypes.INTEGER_CONSTANT; }
    {FLOAT}                                                     { return DTOTypes.FLOAT_CONSTANT; }

    {LINE_COMMENT}                                              { return DTOTypes.LINE_COMMENT; }
    {BLOCK_COMMENT}                                             { return DTOTypes.BLOCK_COMMENT; }
    {IDENTIFIER}                                                { return DTOTypes.IDENTIFIER; }

    ","                                                         { return DTOTypes.COMMA; }
    "."                                                         { return DTOTypes.DOT; }
    "@"                                                         { return DTOTypes.AT; }
    "="                                                         { return DTOTypes.EQUALS; }
    ":"                                                         { return DTOTypes.COLON; }
    ";"                                                         { return DTOTypes.SEMICOLON; }
    "("                                                         { return DTOTypes.PAREN_L; }
    ")"                                                         { return DTOTypes.PAREN_R; }
    "["                                                         { return DTOTypes.BRACKET_L; }
    "]"                                                         { return DTOTypes.BRACKET_R; }
    "{"                                                         { return DTOTypes.BRACE_L; }
    "}"                                                         { return DTOTypes.BRACE_R; }

    ({CRLF}|{WHITE_SPACE})+                                     { return TokenType.WHITE_SPACE; }

    [^]                                                         { return TokenType.BAD_CHARACTER; }
}
