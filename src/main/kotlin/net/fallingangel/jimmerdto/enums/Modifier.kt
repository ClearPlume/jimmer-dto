package net.fallingangel.jimmerdto.enums

enum class Modifier(val level: Level, val order: Int) {
    Input(Level.Dto, 2),
    Specification(Level.Dto, 2),
    Sealed(Level.Dto, -1),
    Unsafe(Level.Dto, 0),
    Fixed(Level.Both, 1),
    Static(Level.Both, 1),
    Dynamic(Level.Both, 1),
    Fuzzy(Level.Both, 1),
    Default(Level.Variant, 0);

    enum class Level {
        Both, Dto, Prop, Variant
    }
}