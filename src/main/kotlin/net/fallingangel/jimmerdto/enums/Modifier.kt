package net.fallingangel.jimmerdto.enums

enum class Modifier(val level: Level, val order: Int) {
    Input(Level.Dto, 2),
    Specification(Level.Dto, 2),
    Unsafe(Level.Dto, 0),
    Fixed(Level.Both, 1),
    Static(Level.Both, 1),
    Dynamic(Level.Both, 1),
    Fuzzy(Level.Both, 1);

    enum class Level {
        Both, Dto
    }
}