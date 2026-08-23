grammar DTO;

@header {
package net.fallingangel.jimmerdto.psi;
}

// Parser
dtoFile
    :
    exportStatement? importStatement* dto* EOF
    ;

exportStatement
    :
    Export qualifiedName (Arrow Package qualifiedName)?
    ;

importStatement
    :
    Import qualifiedName (Dot groupedImport | As alias)?
    ;

groupedImport
    :
    LBrace importedType (Comma importedType)* RBrace
    ;

importedType
    :
    imported (As alias)?
    ;

dto
    :
    annotation*
    Modifier*
    dtoName
    implements?
    dtoBody
    ;

dtoName
    :
    Identifier
    ;

implements
    :
    Implements typeRef (Comma typeRef)*
    ;

dtoBody
    :
    LBrace
    ((macro | aliasGroup | positiveProp | negativeProp | userProp | polymorphic) (Comma | SemiColon)?)*
    RBrace
    ;

macro
    :
    Hash macroName macroArgs? (QuestionMark | ExclamationMark)?
    ;

macroName
    :
    Identifier
    ;

macroArgs
    :
    LParen macroArg? (Comma macroArg)* RParen
    ;

macroArg
    :
    Identifier
    ;

aliasGroup
    :
    As LParen Power? original? Dollar? Arrow? replacement? RParen aliasGroupBody
    ;

original
    :
    Identifier
    ;

replacement
    :
    Identifier
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
    propName
    propFlag?
    propArg?
    (QuestionMark | ExclamationMark | Star)?
    (As alias)?
    propBody?
    ;

propFlag
    :
    Slash Identifier? Power? Dollar?
    ;

propArg
    :
    LParen value? (Comma value)* Comma? RParen
    ;

value
    :
    Identifier
    ;

propBody
    :
    (annotation* implements? dtoBody) | Arrow enumBody
    ;

negativeProp
    :
    Minus propName
    ;

userProp
    :
    annotation* propName Colon typeRef? Equals? defaultValue?
    ;

defaultValue
    :
    BooleanLiteral | IntegerLiteral | StringLiteral | FloatingPointLiteral | Null
    ;

propName
    :
    Identifier | Like | Null | Desc | Asc
    ;

// TODO propConfig 按名字拆 token
//  propConfig
//      :
//      whereConfig | orderByConfig | filterConfig | ... | unknownConfig
//      ;
//  unknownConfig
//      :
//      PropConfigName (LParen ... RParen)?
//      ;
//  PropConfigName 是笼统的 '!' Identifier，导致 propConfig 的三个 LParen 分支靠顺序消歧，!orderBy(firstName)（无 asc/desc）被第一分支 LParen qualifiedName RParen 吞掉，PSI 结构与语义不符。
//  改为每个配置名一个 lexer token，propConfig 按名字分派产生式。同时保留 PropConfigName 作为兜底，annotator 只要看到 unknownConfig，就直接“Unknown prop config name”。
//  propConfig 节点提供自己的初始解析空间，DTOQualifiedName.initialSpace 只做转发。
//  拆分时 intPair 一并改为 intArgs（IntegerLiteral (Comma IntegerLiteral)*），参数个数校验从语法层移到 annotator，措辞与其余 propConfig 对齐。
propConfig
    :
    PropConfigName
    (
        LParen (qualifiedName | intPair) RParen |
        LParen orderByArgs RParen |
        LParen whereArgs RParen
    )
    ;

whereArgs
    :
    predicate? ((And | Or) predicate)*
    ;

orderByArgs
    :
    orderItem? ((Comma) orderItem)*
    ;

intPair
    :
    IntegerLiteral (Comma IntegerLiteral)?
    ;

predicate
    :
    LParen orPredicate RParen | compare | nullity
    ;

orPredicate
    :
    andPredicate (Or andPredicate)*
    ;

andPredicate
    :
    predicate (And predicate)*
    ;

compare
    :
    qualifiedName compareSymbol? propValue?
    ;

compareSymbol
    :
    Equals | NotEquals1 | NotEquals2 | LessThan | LessThanEquals | GreaterThan | GreaterThanEquals | Like | ILike
    ;

nullity
    :
    qualifiedName Is Not? Null?
    ;

propValue
    :
    BooleanLiteral | CharacterLiteral | SqlStringLiteral | IntegerLiteral | FloatingPointLiteral
    ;

orderItem
    :
    qualifiedName orderDirection?
    ;

orderDirection
    :
    Asc | Desc | Identifier
    ;

annotation
    :
    At qualifiedName (LParen (annotationValue | annotationParameter)? (Comma (annotationValue | annotationParameter))* RParen)?
    ;

annotationParameter
    :
    Identifier Equals annotationValue?
    ;

annotationValue
    :
    annotationSingleValue | annotationArrayValue
    ;

annotationSingleValue
    :
    BooleanLiteral |
    CharacterLiteral |
    StringLiteral (Plus StringLiteral)* |
    IntegerLiteral |
    FloatingPointLiteral |
    qualifiedName classSuffix? |
    nestedAnnotation
    ;

annotationArrayValue
    :
    LBrace annotationValue? (Comma annotationValue)* RBrace
    |
    LBracket annotationValue? (Comma annotationValue)* RBracket
    ;

nestedAnnotation
    :
    At? qualifiedName LParen (annotationValue | annotationParameter)? (Comma (annotationValue | annotationParameter))* RParen
    ;

enumBody
    :
    LBrace (enumMapping (Comma | SemiColon)?)* RBrace
    ;

enumMapping
    :
    enumMappingConstant Colon (StringLiteral | IntegerLiteral)
    ;

enumMappingConstant
    :
    Identifier
    ;

classSuffix
    :
    QuestionMark? (Dot | DoubleColon) (Class | Identifier)
    ;

directive
    :
    Hash Identifier
    ;

polymorphic
    :
    directive LBrace (macro | defaultMorphism | typeMorphism)* RBrace
    ;

defaultMorphism
    :
    annotation*
    Default
    classDeclaration?
    implements?
    dtoBody
    ;

typeMorphism
    :
    annotation*
    qualifiedName
    classDeclaration?
    implements?
    dtoBody
    ;

classDeclaration
    :
    Class Identifier
    ;

// Common
qualifiedName
    :
    qualifiedNamePart ('.' qualifiedNamePart)*
    ;

qualifiedNamePart
    :
    Identifier | Like | Null | Desc | Asc
    ;

typeRef
    :
    qualifiedName
    genericArguments?
    QuestionMark?
    ;

genericArguments
    :
    LessThan genericArgument (Comma genericArgument)* GreaterThan
    ;

genericArgument
    :
    Star | (Modifier? typeRef)
    ;

imported
    :
    Identifier
    ;

alias
    :
    Identifier
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
Power: '^';
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
SingleQuote: '\'';
DoubleQuote: '"';

Export: 'export';
Package: 'package';
Import: 'import';
As: 'as';
Implements: 'implements';
Like: 'like';
ILike: 'ilike';
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
    'in' |
    'sealed'
    ;

Default
    :
    'default'
    ;

BooleanLiteral
    :
    'true' | 'false'
    ;

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
    '/**' ('*/' | EOF | ~'/' .*? ('*/' | EOF))
    ;

BlockComment
    :
    ('/*' .*? '*/' | '/*' .*? EOF) -> channel(HIDDEN)
    ;

LineComment
    :
    ('//' ~[\r\n]*) -> channel(HIDDEN)
    ;

SqlStringLiteral
    :
    SingleQuote ( ~'\'' | '\'\'' )* SingleQuote
    ;

CharacterLiteral
	:
	SingleQuote SingleCharacter SingleQuote
	|
	SingleQuote EscapeSequence SingleQuote
	;

fragment
SingleCharacter
	:
	~['\\\r\n]
	;

StringLiteral
	:
	DoubleQuote StringCharacters? DoubleQuote
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
	'-'? ('0' | [1-9][0-9]*)
	;

FloatingPointLiteral
    :
    '-'? [0-9]+ '.' [0-9]+
    ;

ErrorChar
    :
    [\u0000-\uD7FF\uE000-\uFFFF] | [\uD800-\uDBFF][\uDC00-\uDFFF]
    ;
