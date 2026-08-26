# Changelog

## Unreleased

## [0.0.8] - Unreleased

### Added

* Annotation parameter validation: undeclared parameters, missing required parameters, type mismatch, duplicated parameters, `value` shorthand position
* Quick fixes for the above: generate missing parameters with type-based placeholders (candidate lists for boolean and enum), remove or relocate the `value` shorthand, remove duplicated and undeclared parameters
* Gutter icon on entities navigating to bound DTO files
* Entity rename synchronizes the file name of DTO files bound without `export`
* Import of built-in types is rejected
* Paired insertion and deletion of angle brackets for generic arguments
* Folding support for generic argument lists
* Dedicated error message for double-quoted string literals in SQL predicates
* Validation of the `!` modifier against DTO type modifiers and base property nullability
* Validation of the `*` modifier against base property recursiveness, function invocation, child body, and DTO type modifiers
* Validation that an enum body is only specified for enum properties
* Validation of the `?` modifier against DTO type modifiers, the `flat` function, base property nullability, and enclosing flat nullability
* Validation for `!where` predicates: property path resolution, operator applicability to the operand type, and literal type and range
* Validation for `!orderBy` items: property path resolution and order direction
* Pressing Enter inside an unterminated `/*` or `/**` completes the comment and continues it with `*`
* Incomplete prop config arguments are now reported as errors — a missing `null` after `is`, a comparison with no operator or value, and empty argument lists
* Missing arguments are reported for `!filter`, `!recursion`, `!fetchType`, `!limit`, `!batch` and `!depth`
* Duplicated prop configs on the same property are reported — only the last one takes effect, the others are silently ignored
* The filter class of `!filter` is checked against the target entity of the property — `Book.authors` requires a filter declared on `Author`
* The recursion class of `!recursion` is checked against the target entity of the property
* Prop configs are rejected on function properties — `id()`, `flat()` and `fold()`
* Prop configs are rejected on input and specification DTOs
* Applicability of each prop config to the property kind is checked — `!where` requires a nullable association, `!fetchType` an associated reference, `!orderBy` / `!filter` / `!limit` / `!batch` an associated list, `!recursion` / `!depth` a recursive property
* Mutually exclusive prop configs are reported — `!filter` against `!where` and `!orderBy`, `!recursion` against `!depth`
* The fetch mode of `!fetchType` is checked against `SELECT`, `JOIN_IF_NO_CACHE` and `JOIN_ALWAYS` — `AUTO` is not accepted here
* Numeric arguments of `!limit`, `!batch` and `!depth` are range-checked, and literals outside the Int range are reported instead of crashing the annotator
* Annotations forbidden by the DTO language are reported — `Nullable` and `NonNull` by simple name, `Null`, `NotNull` and `TNullable` when they resolve to a type other than the one the DTO language accepts, and Jimmer's own annotations outside the `client` and `jackson` packages
* Report an error when `flat` or `fold` has no property body, or when any other property function has one, with quick fixes to add or remove the body
* Property paths now resolve implicit association id segments such as `storeId`
* Implicit association id segments navigate to both the association property and the target id property
* Find Usages on a reference association property now finds implicit id segments in property paths
* Mark Directory As now supports marking DTO source roots
* DTO source roots are registered automatically on Maven and Gradle project import, from `jimmer.dto.dirs` and `jimmer.dto.testDirs`, defaulting to `src/main/dto` and `src/test/dto`
* When jumping from an entity to its DTO file, a list is shown to choose from if the entity has more than one DTO file
* Find Usages and usage highlighting on JVM types such as `Integer`, `Object` and `List` now include their DTO builtin aliases (`Int`, `Any`, `MutableList`) in `.dto` files
* Find Usages on a Java entity's bean-style getter now lists its references in `.dto` files
* DTO property resolution now refreshes after a Gradle or Maven sync, without requiring an edit to the .dto file
* `New → Directory` now suggests configured Jimmer DTO source directories that have not been created yet
* `New → Jimmer` DTO File is available in the New menu inside DTO source roots
* Added completion for types in `implements` clauses
* Highlight unused imports and support Optimize Imports for DTO files
* Optimize Imports shows a notification when no unused imports are found
* Rename an import alias from any of its usages in the file
* Ctrl+Click on an import alias navigates to its declaration in the import statement
* Import aliases in DTO files now support navigation, find usages, and rename
* Show an import hint for unresolved names that match importable classes
* Quick fix for adding annotation parameters now inserts simple class names, with an import hint on the inserted name
* Report an error when the class specified by `!filter` or `!recursion` does not implement the required interface
* Completion for `!filter` and `!recursion` argument now suggests implementations of the required base type
* Completion for `!fetchType` argument now suggests the available fetch types
* Reports a name other than `class` after `::` in an annotation value
* Reports an annotation argument that is not a class literal, with fixes to correct it
* Report qualified names that can be shortened when the type is already imported, with a quick fix to remove the redundant prefix
* Reports an annotation argument that is not a compile-time constant
* Annotation parameter values now offer completion for classes, packages, enum constants and other referable names, boolean parameters offer `true` and `false`
* Report `@KotlinDto` as unused when it is written outside a DTO type declaration, or when the module is compiled by jimmer-apt
* Report annotations on DTO properties that the Jimmer compiler silently drops because their `@Target` does not accept a property site
* Improved completion and error recovery for `class` declarations in polymorphic DTO branches
* Added `class` keyword completion for polymorphic DTO branches
* Type branches inside a `#types` block now offer completion for the entity's subtypes
* Added completion for type names inside grouped imports
* Argument lists are now indented when written across multiple lines, covering annotations (including nested ones), macros, prop functions, and generic arguments
* Nullable hints now appear on negative props and on property paths in `prop-config`
* Property and annotation parameter completions now show their type
* Report an error when a property modifier appears in an invalid position with quick fixes

### Changed

* The action id for opening the DTO file has changed(CreateJimmerDtoFile → OpenDtoFileAction)
* The "JimmerDto File" action will not be removed in 0.0.8 as previously announced; its entity binding lookup now goes through the index and supports `export`
* Built-in type resolution no longer depends on the entity's language; it is determined by the module's build setup
* Grammar: empty annotation parameter lists and array values allowed; `nestedAnnotation` accepts the same trailing items as `annotation`
* Syntax and annotator colors now follow the active editor color scheme instead of hardcoded values
* Negative properties are rendered with a strikethrough instead of the unused-element style
* DTO class cleanup no longer fails while the IDE is indexing, and no longer appears in the undo history
* `user-prop` type completion no longer freezes on short prefixes, and now matches camel humps

### Fixed

* Import insertion and formatting when importing a type through completion
* The "JimmerDto File" action locates the dto root from the source root's parent instead of the content root
* The "JimmerDto File" action now works on Kotlin entities and is available from the editor context menu, not just the project view
* `user-prop` types resolve even when the entity fails to resolve
* Duplicate import detection accounts for aliases and grouped imports
* Unterminated block comments and doc comments are lexed correctly
* Uncommenting a block comment no longer leaves a stray closing marker
* Non-ASCII characters no longer break parsing of incomplete comments
* Line comments are no longer forced to the first column when toggling with `Ctrl + /`
* Rename disabled for macro names and function property names
* The entity-to-DTO navigation action is now available on entity files containing other top-level declarations
* Java entities: boolean getters with an `is` prefix now resolve to the property name Jimmer actually generates, respecting the `keepIsPrefix` option
* The `as` keyword in a grouped import is no longer highlighted as an annotation
* Built-in types are no longer offered for import or inserted as import statements
* Fixed `like` and `notLike` incorrectly accepting properties of non-string types
* `class` is now suggested only where the annotation parameter expects a class literal
* Polymorphic DTO branches now require either `default` or a target type, matching the compiler grammar
* Completing a class that resides in the entity's package no longer inserts a redundant import statement
* Fixed DTO files losing reference resolution, documentation, and navigation until project reopen after a transient error in the export statement

## [0.0.7.50] - 2026-07-21

### Added

* Polymorphic DTO syntax infrastructure: grammar rules, PSI elements, formatting
* Polymorphic DTO morphism syntax error messages
* `parents` recognition for `@Entity` inheritance hierarchy
* `#exhaustive` initial support: context dispatch, completion, highlighting
* `sealed` modifier validation: forbidden on specification, requires `#types` block

### Changed

* Rebuilt L* intermediate representation: removed zero-consumer abstractions, eliminated model-level generics, nested domain classifications
* Extracted Jimmer semantic extensions to dedicated `lsi/jimmer` package
* Upgraded Gradle 9.6.1, Kotlin 2.4.0, Jimmer 0.11.0
* Compatibility range narrowed to 2024.3 ~ 2026.2

### Fixed

* `qualifiedNamePart` Kotlin-side prelude package search scope
* `LAnnotation.Param.Value` comparison via `eq` methods

## [0.0.7.49] - 2026-07-11

### Added

* `fold` function support: validation, completion, and hint info
* Spec function validation
* Function existence validation
* Macro must be the first element, with a clear error if preceded by non-macro
  elements ([#68](https://github.com/ClearPlume/jimmer-dto/issues/68))
* Error when declaring user-defined properties under `flat` function
* Error when `->` is missing in `aliasGroup`

### Changed

* Refactored function constraint system into a unified `Function` enum
* Replaced `propPath` with `containingLClass` system
* Moved entity class reference from `DTOFile` down to `DTODto`
* Made `Arrow` optional in `aliasGroup`, promoted `original`/`replacement` to standalone rules
* Changed `LanguageProcessor`'s `resolvedType` from instance field to call-local state
* Improved `L*` layer identity semantics
* Added `containingLClass` to `LProperty`

### Fixed

* Macro validation no longer produces duplicate error annotations
* `aliasGroup` alias-pattern validation aligned with compiler error messages
* Fixed annotation parameter type checking: unified scalar/array validation, resolved nested annotation qualified names, removed extra comma in
  generation

## [0.0.7.48] - 2026-03-26

### Added

* 2026.1 is supported

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.47] - 2025-12-20

### Added

* 2025.3 is supported

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.46] - 2025-08-05

### Added

* 2025.2 is supported

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.45] - 2025-04-20

### Added

* Issue [#50](https://github.com/ClearPlume/jimmer-dto/issues/50): In `prop-config`, an error message is displayed for the join table operation
* DTO duplicate definition check
* Super interface validation
* Macro validation
* Annotation parameter verification

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.44] - 2025-04-07

### Added

* Issue [#55](https://github.com/ClearPlume/jimmer-dto/issues/55): Add semantic check and quick fixes to DTO modifiers
* In `prop-path`, directly accessing the ID now triggers a prompt indicating it should be changed to use view
* `user-prop` support default values

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#49](https://github.com/ClearPlume/jimmer-dto/issues/49): An error message is added when the type of `user-prop` is non-null and its
  default
  value cannot be inferred
* Issue [#51](https://github.com/ClearPlume/jimmer-dto/issues/51): Alias definition for direct children is prohibited in `alias-group`

## [0.0.7.43] - 2025-04-01

### Added

* Annotation parameter value hints (annotation type parameters)
* Annotation parameter reference parsing, prompting, verification, refactoring, and quick fixing

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#59](https://github.com/ClearPlume/jimmer-dto/issues/59): Fixed when the first statement is not at the beginning of the DTO file, it
  cannot
  be extracted correctly

## [0.0.7.42] - 2025-03-26

### Added

* Annotation parameter value hints (annotation type parameters)

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#58](https://github.com/ClearPlume/jimmer-dto/issues/58): Adjust the way to determine the language of the project

## [0.0.7.412] - 2025-03-25

### Added

* Added support for k2 mode

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.41] - 2025-03-21

### Added

* When the imported class is annotation, apply annotation highlighting to it

### Changed

* Use ANTLR to replace BNF, improve syntax parsing and code prompting capabilities

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#44](https://github.com/ClearPlume/jimmer-dto/issues/44): Correct package prompts when importing group
* Issue [#54](https://github.com/ClearPlume/jimmer-dto/issues/54): `prop-function` arg reference analysis failed except id and flat
* Issue [#48](https://github.com/ClearPlume/jimmer-dto/issues/48): Type judgment error when using empty strings in `enum-mapping`
* Issue [#47](https://github.com/ClearPlume/jimmer-dto/issues/47): Generic parameter mismatch

## [0.0.7.40] - 2025-03-07

### Added

* Provide suggestions for incorrect `prop-config` names
* Suggestions for `prop-config` parameters

### Changed

* Contextual `prop-config` suggestions (excluding args)

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

## [0.0.7.39] - 2025-02-28

### Changed

* Adjusted the limit property to follow common semantics

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Removed

* Removed the offset `prop-config`

## [0.0.7.38] - 2025-02-23

### Added

* 2025.1 EAP is supported
* Code hints for `prop-config` (excluding args)
* Added syntax highlighting support for `like`, `ilike`, `asc`, and `desc` keywords

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Fixed PsiReference resolution failures for generated classes in `export` statements with `package` clauses

## [0.0.7.37] - 2025-02-16

### Added

* Issue [#35](https://github.com/ClearPlume/jimmer-dto/issues/35): Added Quick Documentation for macros, displaying a list of properties
  mentioned by
  the macros
* Issue [#36](https://github.com/ClearPlume/jimmer-dto/issues/36): Added a quick fix for correcting macro name
* Issue [#39](https://github.com/ClearPlume/jimmer-dto/issues/39): Added reference resolution to DTO name

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#38](https://github.com/ClearPlume/jimmer-dto/issues/38): Fixed an issue where macro parameter hints would not appear if the macro
  parameter
  list was empty
* Issue [#37](https://github.com/ClearPlume/jimmer-dto/issues/37): Fixed the issue where there should be no space before "*?!" within a prop
* Issue [#31](https://github.com/ClearPlume/jimmer-dto/issues/31): Fixed an issue where missing classes or packages in export and import
  statements
  were not producing error messages
* Issue [#32](https://github.com/ClearPlume/jimmer-dto/issues/32): Fixed an issue where the corresponding DTO class was not being deleted upon
  saving
  the DTO file
* Issue [#29](https://github.com/ClearPlume/jimmer-dto/issues/29): Fixed an issue where an `import` statement was incorrectly inserted when
  completing
  class suggestions in `export` statements
* Issue [#41](https://github.com/ClearPlume/jimmer-dto/issues/41): Improved type hinting in prop suggestions to include information about
  nullability
* Issue [#40](https://github.com/ClearPlume/jimmer-dto/issues/40): Enhanced support for numeric literals to include suffixes like l, L, d, D, f,
  F,
  and prefixes for hexadecimal and binary representations, as well as exponent notation

## [0.0.7.36] - 2025-02-12

### Added

* Complete syntax parsing and coloring for prop-config

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#33](https://github.com/ClearPlume/jimmer-dto/issues/33): Remove line break between prop annotation and prop body
* Issue [#34](https://github.com/ClearPlume/jimmer-dto/issues/34): Fix escape recognition in strings and characters
* Fix param highlighting when multiple args in prop function

## [0.0.7.35] - 2025-02-10

### Added

* Implement `PsiReferenceContributor`, complete reference association
* Reference parsing of imported elements is supported
* Blank lines after comments are formatted
* Reference parsing of args in `#allScalars`
* Improved messaging when `this` and `simple entity class name` co-occur in `#allScalars` arg list

### Fixed

* Issue [#28](https://github.com/ClearPlume/jimmer-dto/issues/28): Improve the formatting of enum mapping
* Issue [#27](https://github.com/ClearPlume/jimmer-dto/issues/27): Fixed hint for child prop in multi-level props
* Fixed an error where there were no elements in the enum-mapping when the code was colored
* Enhanced error messaging and quick fix suggestions for `#allScalars` when args are missing
* Enhanced error messaging for function when args are missing
* Enhanced error messaging for alias-group when alias-pattern is missing

## [0.0.7.34] - 2025-01-14

### Added

* Support for folding child content within curly braces

### Fixed

* Issue [#27](https://github.com/ClearPlume/jimmer-dto/issues/27): Fixed the parsing error in the alias-group in version 2024.3.1.1
* Fixed the parsing error of the `null` method in the specification

## [0.0.7.33] - 2025-01-09

### Fixed

* Issue [#26](https://github.com/ClearPlume/jimmer-dto/issues/26): Fixed incorrect indentation levels after pressing Enter
* Issue [#25](https://github.com/ClearPlume/jimmer-dto/issues/25): Fixed property hints were missing in associated property body and flat body
  due to
  changes in the BNF structure
* Issue [#24](https://github.com/ClearPlume/jimmer-dto/issues/24): Fixed spaces could not follow `as` in the alias-group

## [0.0.7.32] - 2025-01-08

### Added

* Formatting: Remove spaces around the parentheses `()`, `[]`, `<>`
* Formatting: Space around `->`
* Formatting: Space around the `as` keyword
* Formatting: A space after the `export` keyword in export statements
* Formatting: A newline and a tab before `->` in export statements
* Formatting: Indentation of child elements within Dto body, associated property body, and `as` block body
* Formatting: Handling spaces before the opening curly brace
* Formatting: Adding blank lines before and after properties in Dto body

### Fixed

* Support for missing `in` and `out` keywords when using generics

## [0.0.7.31] - 2025-01-07

### Added

* Basic completion of formatting controls

### Fixed

* Incorrect removal of `@` when using fully qualified mode for annotation hints
* Failure to trigger package imports after removing qualified-type-name
* Fix the impact of whitespace on the logic of obtaining export and import package structures

## [0.0.7.30] - 2025-01-01

### Added

* Nested annotation hints, nested annotation parameter hints
* Annotation parameter hints

### Fixed

* Incorrect hint information listed when using fully qualified names to input annotations
* Import hints sometimes ineffective
* Fix the impact of whitespace on the logic of obtaining export and import package structures

## [0.0.7.29] - 2024-12-30

### Added

* Annotation import basically complete

## [0.0.7.28] - 2024-11-15

### Fixed

* Issue [#23](https://github.com/ClearPlume/jimmer-dto/issues/23): Fix nullable data handling in DTOAnnotator

## [0.0.7.27] - 2024-11-14

### Added

* Support for 2024.3, Removal of 2022.2, <b>Support for 2022.3 will be removed when 2024.4 is released

## [0.0.7.26] - 2024-09-26

### Fixed

* Issue [#21](https://github.com/ClearPlume/jimmer-dto/issues/21): Update CreateJimmerDtoFile#getActionUpdateThread method to return
  ActionUpdateThread.BGT

## [0.0.7.25] - 2024-09-09

### Fixed

* Issue [#16](https://github.com/ClearPlume/jimmer-dto/issues/16): Remove child structure hints for recursive prop
* Issue [#19](https://github.com/ClearPlume/jimmer-dto/issues/19): Add warning messages for non-existent or package targets in export/import

## [0.0.7.24] - 2024-08-20

### Added

* Add error message for usage of non-existent prop in entities

### Changed

* Update warning message for duplicate names between user-prop and entity prop

### Fixed

* Issue [#16](https://github.com/ClearPlume/jimmer-dto/issues/16): Add error message and quick fix for unstructured association prop
* Issue [#15](https://github.com/ClearPlume/jimmer-dto/issues/15): Add warning and quick fix for using generated DTOs as user-prop type in Jimmer
* Issue [#14](https://github.com/ClearPlume/jimmer-dto/issues/14): Fix issue where null method in specification DTO are not recognized as method
* Issue [#13](https://github.com/ClearPlume/jimmer-dto/issues/13): Update InsertEntityPropAction#getActionUpdateThread method to return
  ActionUpdateThread.BGT

## [0.0.7.23] - 2024-08-19

### Added

* Provide quick fix suggestions for errors in macro args

## [0.0.7.22] - 2024-06-26

### Fixed

* Issue [#11](https://github.com/ClearPlume/jimmer-dto/issues/11): Add error message and repair options for duplicate names between user prop and
  entity prop
* Issue [#10](https://github.com/ClearPlume/jimmer-dto/issues/10): Fix error and improve hints for macro arg involving ancestor types

## [0.0.7.21] - 2024-05-10

### Added

* Implement type name suggestions for un-imported user prop types during input
* Add error message and quick import prompt for user prop type not in the built-in list

## [0.0.7.20] - 2024-05-08

### Fixed

* Issue [#12](https://github.com/ClearPlume/jimmer-dto/issues/12): Adjust lexical rules to avoid conflicts between modifiers and prop names
