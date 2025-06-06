# Changelog

## Unreleased

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

* Issue [#49](https://github.com/ClearPlume/jimmer-dto/issues/49): An error message is added when the type of `user-prop` is non-null and its default
  value cannot be inferred
* Issue [#51](https://github.com/ClearPlume/jimmer-dto/issues/51): Alias definition for direct children is prohibited in `alias-group`

## [0.0.7.43] - 2025-04-01

### Added

* Annotation parameter value hints (annotation type parameters)
* Annotation parameter reference parsing, prompting, verification, refactoring, and quick fixing

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#59](https://github.com/ClearPlume/jimmer-dto/issues/59): Fixed when the first statement is not at the beginning of the DTO file, it cannot
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

* Issue [#35](https://github.com/ClearPlume/jimmer-dto/issues/35): Added Quick Documentation for macros, displaying a list of properties mentioned by
  the macros
* Issue [#36](https://github.com/ClearPlume/jimmer-dto/issues/36): Added a quick fix for correcting macro name
* Issue [#39](https://github.com/ClearPlume/jimmer-dto/issues/39): Added reference resolution to DTO name

### Deprecated

* Deprecate 'CreateOrJumpToJimmerDtoFile'; to be removed in 0.0.8

### Fixed

* Issue [#38](https://github.com/ClearPlume/jimmer-dto/issues/38): Fixed an issue where macro parameter hints would not appear if the macro parameter
  list was empty
* Issue [#37](https://github.com/ClearPlume/jimmer-dto/issues/37): Fixed the issue where there should be no space before "*?!" within a prop
* Issue [#31](https://github.com/ClearPlume/jimmer-dto/issues/31): Fixed an issue where missing classes or packages in export and import statements
  were not producing error messages
* Issue [#32](https://github.com/ClearPlume/jimmer-dto/issues/32): Fixed an issue where the corresponding DTO class was not being deleted upon saving
  the DTO file
* Issue [#29](https://github.com/ClearPlume/jimmer-dto/issues/29): Fixed an issue where an `import` statement was incorrectly inserted when completing
  class suggestions in `export` statements
* Issue [#41](https://github.com/ClearPlume/jimmer-dto/issues/41): Improved type hinting in prop suggestions to include information about
  nullability
* Issue [#40](https://github.com/ClearPlume/jimmer-dto/issues/40): Enhanced support for numeric literals to include suffixes like l, L, d, D, f, F,
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
* Issue [#25](https://github.com/ClearPlume/jimmer-dto/issues/25): Fixed property hints were missing in associated property body and flat body due to
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