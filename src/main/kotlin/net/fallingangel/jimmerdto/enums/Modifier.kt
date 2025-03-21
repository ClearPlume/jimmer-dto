package net.fallingangel.jimmerdto.enums

enum class Modifier(val level: Level) {
    Input(Level.Dto),
    Specification(Level.Dto),
    Unsafe(Level.Dto),
    Fixed(Level.Both),
    Static(Level.Both),
    Dynamic(Level.Both),
    Fuzzy(Level.Both);

    enum class Level {
        Both, Dto
    }
}