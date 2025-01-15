# Changelog

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