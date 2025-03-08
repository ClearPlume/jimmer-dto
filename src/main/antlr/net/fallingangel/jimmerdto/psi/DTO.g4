grammar DTO;

@header {
package net.fallingangel.jimmerdto.psi;
}

// Parser
dtoFile
    :
    exportStatement?
    (importStatements += importStatement)*
    (dtos += dto)*
    EOF
    ;

exportStatement
    :
    'export' qualifiedName
    ('->' 'package' qualifiedName)?
    ;

importStatement
    :
    'import' qualifiedName
    (
        '.' '{' importedTypes += importedType (',' importedTypes += importedType)* '}' |
        'as' alias = Identifier
    )?
    ;

importedType
    :
    name = Identifier ('as' alias = Identifier)?
    ;

dto
    :
    (annotations += annotation)*
    (modifiers += modifier)*
    name=Identifier
    ('implements' interfaces += typeRef (',' interfaces += typeRef)*)?
    body=dtoBody
    ;

dtoBody
    :
    '{'
    ((userProp | macro | positiveProp | negativeProp | aliasGroup) (',' | ';')?)*
    '}'
    ;

macro
    :
    '#' name = Identifier
    ('(' args += qualifiedName (',' args += qualifiedName)* ')')?
    (optional = '?' | required = '!')?
    ;

aliasGroup
    :
    pattern = aliasPattern '{' (macros += macro)* (props += positiveProp)* '}'
    ;

aliasPattern
    :
    'as' '('
    (prefix = '^')?
    (original = Identifier)?
    (suffix = '$')?
    (translator = '->')
    (replacement = Identifier)?
    ')'
    ;

positiveProp
    :
    (configs += propConfig | annotations += annotation)*
    '+'?
    modifier?
    (
        name = (Identifier | 'like' | 'null')
        (flag = '/' (insensitive = 'i')? (prefix = '^')? (suffix = '$')?)?
        ('(' args += Identifier (',' args += Identifier)* ','? ')')?
    )
    (optional = '?' | required = '!' | recursive = '*')?
    ('as' alias = Identifier)?
    (
        (bodyAnnotations += annotation)*
        ('implements' interfaces += typeRef (',' interfaces += typeRef)*)?
        dtoBody
        |
        '->' enumBody
    )?
    ;

negativeProp
    :
    '-' prop = Identifier
    ;

userProp
    :
    (annotations += annotation)*
    prop = Identifier ':' typeRef
    ;

propConfig
    :
    PropConfigName
    (
        '(' predicate (('and' | 'or') predicate)* ')' |
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
    '=' | '<>' | '!=' | '<' | '<=' | '>' | '>=' | 'like' | 'ilike'
    ;

nullity
    :
    qualifiedName 'is' 'not'? 'null'
    ;

propPath
    :
    parts += Identifier ('.' parts += Identifier)*
    ;

propValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringToken = SqlStringLiteral |
    integerToken = IntegerLiteral |
    floatingPointToken = FloatingPointLiteral |
    ;

orderItem
    :
    propPath ('asc' | 'desc')?
    ;

annotation
    :
    '@' typeName = qualifiedName ('(' annotationArguments? ')')?
    ;

annotationArguments
    :
    defaultArgument = annotationValue (',' namedArguments += annotationNamedArgument)*
    |
    namedArguments += annotationNamedArgument (',' namedArguments += annotationNamedArgument)*
    ;

annotationNamedArgument
    :
    name = Identifier '=' value = annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringTokens += StringLiteral ('+' stringTokens += StringLiteral)* |
    integerToken = IntegerLiteral |
    floatingPointToken = FloatingPointLiteral |
    qualifiedPart = qualifiedName classSuffix? |
    annotationPart = annotation |
    nestedAnnotationPart = nestedAnnotation
    ;

annotationArrayValue
    :
    '{' elements += annotationSingleValue (',' elements += annotationSingleValue)* '}'
    |
    '[' elements += annotationSingleValue (',' elements += annotationSingleValue)* ']'
    ;

nestedAnnotation
    :
    typeName = qualifiedName '(' annotationArguments? ')'
    ;

enumBody
    :
    '{' (mappings += enumMapping (','|';')?)+ '}'
    ;

enumMapping
    :
    constant = Identifier ':' value = (StringLiteral | IntegerLiteral)
    ;

classSuffix
    :
    '?'? ('.' | '::') 'class'
    ;

// Common
qualifiedName
    :
    Identifier ('.' parts += Identifier)*
    ;

modifier
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

typeRef
    :
    qualifiedName
    ('<' genericArguments += genericArgument (',' genericArguments += genericArgument)? '>')?
    (optional = '?')?
    ;

genericArgument
    :
    wildcard = '*' |
    modifier? typeRef
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

PropConfigName
    :
    '!' Identifier
    ;