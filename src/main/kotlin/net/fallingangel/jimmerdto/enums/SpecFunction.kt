package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.structure.ArgType
import net.fallingangel.jimmerdto.structure.ArgType.*
import net.fallingangel.jimmerdto.structure.ArgType.Companion.and
import net.fallingangel.jimmerdto.structure.ArgType.Companion.or

enum class SpecFunction(val expression: String, val whetherMultiArg: Boolean, val argType: ArgType) {
    Eq("eq", true, Scalar),
    Ne("ne", false, Scalar),
    Gt("gt", false, Scalar),
    Ge("ge", false, Scalar),
    Lt("lt", false, Scalar),
    Le("le", false, Scalar),
    Like("like", true, ArgType.String and (ArgType.Null or ArgType.NotNull)),
    NotLike("notLike", false, ArgType.String and (ArgType.Null or ArgType.NotNull)),
    Null("null", true, Prop),
    NotNull("notNull", true, Prop),
    ValueIn("valueIn", true, Scalar),
    ValueNotIn("valueNotIn", false, Scalar),
    AssociatedIdEq("associatedIdEq", true, Association),
    AssociatedIdNe("associatedIdNe", false, Association),
    AssociatedIdIn("associatedIdIn", true, Association),
    AssociatedIdNotIn("associatedIdNotIn", false, Association)
}