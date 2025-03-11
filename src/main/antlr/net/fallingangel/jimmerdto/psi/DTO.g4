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
    (Arrow Package qualifiedName)?
    ;

importStatement
    :
    Import qualifiedName
    (
        Dot LBrace importedType (Comma importedType)* RBrace |
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
    (Implements typeRef (Comma typeRef)*)?
    dtoBody
    ;

dtoBody
    :
    LBrace
    ((macro | aliasGroup | positiveProp | negativeProp | userProp) (Comma | SemiColon)?)*
    RBrace
    ;

macro
    :
    Hash Identifier
    macroArgs?
    (QuestionMark | ExclamationMark)?
    ;

macroArgs
    :
    LParen qualifiedName? (Comma qualifiedName)* RParen
    ;

aliasGroup
    :
    As LParen Caret? Identifier? Dollar? Arrow Identifier? RParen aliasGroupBody
    ;

aliasGroupBody
    :
    LBrace macro* positiveProp* RBrace
    ;

positiveProp
    :
    (propConfig | annotation)*
    Plus?
    Modifier?
    (
        propName
        (Slash 'i'? Caret? Dollar?)?
        (LParen Identifier (Comma Identifier)* Comma? RParen)?
    )
    (QuestionMark | ExclamationMark | Star)?
    (As Identifier)?
    propBody?
    ;

propBody
    :
    annotation*
    (Implements typeRef (Comma typeRef)*)?
    dtoBody
    |
    Arrow enumBody
    ;

negativeProp
    :
    Minus propName
    ;

userProp
    :
    annotation*
    propName Colon typeRef
    ;

propName
    :
    Identifier | Like | Null
    ;

propConfig
    :
    PropConfigName
    (
        LParen predicate ((And | Or) predicate)* RParen |
        LParen orderItem ((Comma) orderItem)* RParen |
        LParen Identifier RParen |
        LParen IntegerLiteral (Comma IntegerLiteral)? RParen
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
    Equals | NotEquals1 | NotEquals2 | LessThan | LessThanEquals | GreaterThan | GreaterThanEquals | Like | Ilike
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
    At qualifiedName (LParen (annotationValue | annotationParameter) (Comma annotationParameter)* RParen)?
    ;

annotationParameter
    :
    Identifier Equals annotationValue
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
    StringLiteral (Plus StringLiteral)* |
    IntegerLiteral |
    FloatingPointLiteral |
    qualifiedName classSuffix? |
    annotation |
    nestedAnnotation
    ;

annotationArrayValue
    :
    LBrace annotationValue (Comma annotationValue)* RBrace
    |
    LBracket annotationValue (Comma annotationValue)* RBracket
    ;

nestedAnnotation
    :
    At? qualifiedName LParen (annotationValue | annotationParameter) (Comma annotationParameter)* RParen
    ;

enumBody
    :
    LBrace (enumMapping (Comma|SemiColon)?)+ RBrace
    ;

enumMapping
    :
    Identifier Colon (StringLiteral | IntegerLiteral)
    ;

classSuffix
    :
    QuestionMark? (Dot | DoubleColon) Class
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
Arrow: '->';
Dot: '.';
LBrace: '{';
RBrace: '}';
Comma: ',';
SemiColon: ';';
Hash: '#';
LParen: '(';
RParen: ')';
QuestionMark: '?';
ExclamationMark: '!';
Caret: '^';
Dollar: '$';
Plus: '+';
Slash: '/';
Star: '*';
Minus: '-';
Colon: ':';
Equals: '=';
NotEquals1: '!=';
NotEquals2: '<>';
LessThan: '<';
LessThanEquals: '<=';
GreaterThan: '>';
GreaterThanEquals: '>=';
At: '@';
LBracket: '[';
RBracket: ']';
DoubleColon: '::';

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