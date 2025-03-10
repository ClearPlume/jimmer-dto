grammar DTO;

@header {
package net.fallingangel.jimmerdto.psi;
}

// Parser
dtoFile
    :
    exportStatement?
    importStatement*
    dto*
    EOF
    ;

exportStatement
    :
    Export qualifiedName
    ('->' Package qualifiedName)?
    ;

importStatement
    :
    Import qualifiedName
    (
        '.' '{' importedType (',' importedType)* '}' |
        As alias = Identifier
    )?
    ;

importedType
    :
    Identifier (As Identifier)?
    ;

dto
    :
    annotation*
    Modifier*
    Identifier
    (Implements typeRef (',' typeRef)*)?
    dtoBody
    ;

dtoBody
    :
    '{'
    ((macro | aliasGroup | positiveProp | negativeProp | userProp) (',' | ';')?)*
    '}'
    ;

macro
    :
    '#' Identifier
    ('(' qualifiedName (',' qualifiedName)* ')')?
    ('?' | '!')?
    ;

aliasGroup
    :
    As '(' '^'? Identifier? '$'? '->' Identifier? ')' aliasGroupBody
    ;

aliasGroupBody
    :
    '{' macro* positiveProp* '}'
    ;

positiveProp
    :
    (propConfig | annotation)*
    '+'?
    Modifier?
    (
        propName
        ('/' 'i'? '^'? '$'?)?
        ('(' Identifier (',' Identifier)* ','? ')')?
    )
    ('?' | '!' | '*')?
    (As Identifier)?
    propBody?
    ;

propBody
    :
    annotation*
    (Implements typeRef (',' typeRef)*)?
    dtoBody
    |
    '->' enumBody
    ;

negativeProp
    :
    '-' propName
    ;

userProp
    :
    annotation*
    propName ':' typeRef
    ;

propName
    :
    Identifier | Like | Null
    ;

propConfig
    :
    PropConfigName
    (
        '(' predicate ((And | Or) predicate)* ')' |
        '(' orderItem ((',') orderItem)* ')' |
        '(' Identifier ')' |
        '(' IntegerLiteral (',' IntegerLiteral)? ')'
    )
    ;

predicate
    :
    compare | nullity
    ;

compare
    :
    qualifiedName compareSymbol propValue
    ;

compareSymbol
    :
    '=' | '<>' | '!=' | '<' | '<=' | '>' | '>=' | Like | Ilike
    ;

nullity
    :
    qualifiedName Is Not? Null
    ;

propValue
    :
    BooleanLiteral |
    CharacterLiteral |
    SqlStringLiteral |
    IntegerLiteral |
    FloatingPointLiteral
    ;

orderItem
    :
    qualifiedName (Asc | Desc)?
    ;

annotation
    :
    '@' qualifiedName ('(' (annotationValue | annotationParameter) (',' annotationParameter)* ')')?
    ;

annotationParameter
    :
    Identifier '=' annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    BooleanLiteral |
    CharacterLiteral |
    StringLiteral ('+' StringLiteral)* |
    IntegerLiteral |
    FloatingPointLiteral |
    qualifiedName classSuffix? |
    annotation |
    nestedAnnotation
    ;

annotationArrayValue
    :
    '{' annotationValue (',' annotationValue)* '}'
    |
    '[' annotationValue (',' annotationValue)* ']'
    ;

nestedAnnotation
    :
    '@'? qualifiedName '(' (annotationValue | annotationParameter) (',' annotationParameter)* ')'
    ;

enumBody
    :
    '{' (enumMapping (','|';')?)+ '}'
    ;

enumMapping
    :
    Identifier ':' (StringLiteral | IntegerLiteral)
    ;

classSuffix
    :
    '?'? ('.' | '::') Class
    ;

// Common
qualifiedName
    :
    qualifiedNamePart ('.' qualifiedNamePart)*
    ;

qualifiedNamePart
    :
    Identifier
    ;

typeRef
    :
    qualifiedName
    ('<' genericArgument (',' genericArgument)? '>')?
    '?'?
    ;

genericArgument
    :
    '*' |
    Modifier? typeRef
    ;

// Lexer
Export: 'export';
Package: 'package';
Import: 'import';
As: 'as';
Implements: 'implements';
Like: 'like';
Ilike: 'ilike';
Null: 'null';
And: 'and';
Or: 'or';
Is: 'is';
Not: 'not';
Asc: 'asc';
Desc: 'desc';
Class: 'class';

PropConfigName
    :
    '!' Identifier
    ;

Modifier
    :
    'input' |
    'specification' |
    'unsafe' |
    'fixed' |
    'static' |
    'dynamic' |
    'fuzzy'|
    'out' |
    'in'
    ;

// Lexer
Identifier
    :
    [$A-Za-z_][$A-Za-z_0-9]*
    ;

WhiteSpace
    :
    (' ' | '\u0009' | '\u000C' | '\r' | '\n')+ -> channel(HIDDEN)
    ;

DocComment
    :
    ('/**' .*? '*/')
    ;

BlockComment
    :
    ('/*' .*? '*/') -> channel(HIDDEN)
    ;

LineComment
    :
    ('//' ~[\r\n]*) -> channel(HIDDEN)
    ;

SqlStringLiteral
    :
    '\'' ( ~'\'' | '\'\'' )* '\''
    ;

BooleanLiteral
    :
    'true' | 'false'
    ;

CharacterLiteral
	:
	'\'' SingleCharacter '\''
	|
	'\'' EscapeSequence '\''
	;

fragment
SingleCharacter
	:
	~['\\\r\n]
	;

StringLiteral
	:
	'"' StringCharacters? '"'
	;

fragment
StringCharacters
	:
	StringCharacter+
	;

fragment
StringCharacter
	:
	~["\\\r\n] | EscapeSequence
	;

fragment
EscapeSequence
	:
	'\\' [btnfr"'\\]
    |
    UnicodeEscape // This is not in the spec but prevents having to preprocess the input
    ;

fragment
UnicodeEscape
    :
    '\\' 'u'+  HexDigit HexDigit HexDigit HexDigit
    ;

fragment
HexDigit
    :
    [0-9] | [a-f] | [A-F]
    ;

IntegerLiteral
	:
	'0' | [1-9][0-9]*
	;

FloatingPointLiteral
    :
    [0-9]+ '.' [0-9]+
    ;