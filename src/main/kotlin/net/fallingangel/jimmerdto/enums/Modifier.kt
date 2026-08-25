package net.fallingangel.jimmerdto.enums

enum class Modifier(val positions: Set<Position>, val isInputStrategy: Boolean, val order: Int) {
    Sealed(setOf(Position.Dto), false, -1),
    // TODO body 内递归向下查找，若没有带 ! 的非 id 属性，则冗余
    Unsafe(setOf(Position.Dto), false, 0),
    Input(setOf(Position.Dto), false, 2),
    Specification(setOf(Position.Dto), false, 2),

    Fixed(setOf(Position.Dto, Position.Prop), true, 1),
    Static(setOf(Position.Dto, Position.Prop), true, 1),
    Dynamic(setOf(Position.Dto, Position.Prop), true, 1),
    Fuzzy(setOf(Position.Dto, Position.Prop), true, 1),

    Out(setOf(Position.GenericArgument), false, 0),
    In(setOf(Position.GenericArgument), false, 0);

    enum class Position { Dto, Prop, GenericArgument }
}
