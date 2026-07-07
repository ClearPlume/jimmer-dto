package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.structure.ArgType

enum class Function(val expression: String, val argType: ArgType) {
    Id("id", ArgType.Association),
    Flat("flat", ArgType.Association),

    // TODO 合适的参数类型
    Fold("fold", ArgType.Prop),
}