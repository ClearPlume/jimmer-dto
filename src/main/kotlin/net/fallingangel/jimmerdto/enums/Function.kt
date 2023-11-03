package net.fallingangel.jimmerdto.enums

enum class Function(val expression: String, val argType: ArgType) {
    Id("id", ArgType.Association),
    Flat("flat", ArgType.Association)
}