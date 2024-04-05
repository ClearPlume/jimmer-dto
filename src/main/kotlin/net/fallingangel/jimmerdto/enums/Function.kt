package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.structure.ArgType
import net.fallingangel.jimmerdto.structure.ArgType.Association

enum class Function(val expression: String, val argType: ArgType) {
    Id("id", Association),
    Flat("flat", Association)
}