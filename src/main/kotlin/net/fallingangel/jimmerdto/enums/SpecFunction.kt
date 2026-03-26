package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.structure.ArgType
import net.fallingangel.jimmerdto.structure.ArgType.Companion.and
import net.fallingangel.jimmerdto.structure.ArgType.Companion.or

enum class SpecFunction(val expression: String, val whetherMultiArg: Boolean, val argType: ArgType) {
    Eq("eq", true, ArgType.Scalar),
    Ne("ne", false, ArgType.Scalar),
    Gt("gt", false, ArgType.Scalar),
    Ge("ge", false, ArgType.Scalar),
    Lt("lt", false, ArgType.Scalar),
    Le("le", false, ArgType.Scalar),
    Like("like", true, ArgType.String and (ArgType.Null or ArgType.NotNull)),
    NotLike("notLike", false, ArgType.String and (ArgType.Null or ArgType.NotNull)),
    Null("null", true, ArgType.Prop),
    NotNull("notNull", true, ArgType.Prop),
    ValueIn("valueIn", true, ArgType.Scalar),
    ValueNotIn("valueNotIn", false, ArgType.Scalar),
    AssociatedIdEq("associatedIdEq", true, ArgType.Association),
    AssociatedIdNe("associatedIdNe", false, ArgType.Association),
    AssociatedIdIn("associatedIdIn", true, ArgType.Association),
    AssociatedIdNotIn("associatedIdNotIn", false, ArgType.Association),
}