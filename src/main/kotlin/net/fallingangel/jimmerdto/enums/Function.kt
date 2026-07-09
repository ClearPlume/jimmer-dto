package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.structure.ArgType
import net.fallingangel.jimmerdto.structure.ArgType.*
import net.fallingangel.jimmerdto.structure.ArgType.Companion.and
import net.fallingangel.jimmerdto.structure.ArgType.Companion.not
import net.fallingangel.jimmerdto.structure.ArgType.Companion.or
import net.fallingangel.jimmerdto.util.modifiedBy

enum class Function(
    val expression: String,
    val whetherSpec: Boolean,
    val whetherMultiArg: Boolean,
    val argConstraint: ((DTODto) -> ArgType)? = null,
) {
    Id("id", false, false, { EntityAssociation }),
    Flat("flat", false, false, {
        if (it modifiedBy Modifier.Specification) {
            EntityAssociation or Embeddable or ListAssociation
        } else {
            (EntityAssociation or Embeddable) and ListAssociation.not()
        }
    }),
    Fold("fold", false, false),
    Eq("eq", true, true, { EntityAssociation.not() }),
    Ne("ne", true, false, { EntityAssociation.not() }),
    Gt("gt", true, false, { EntityAssociation.not() }),
    Ge("ge", true, false, { EntityAssociation.not() }),
    Lt("lt", true, false, { EntityAssociation.not() }),
    Le("le", true, false, { EntityAssociation.not() }),
    Like("like", true, true, { StringProp }),
    NotLike("notLike", true, false, { StringProp }),
    Null("null", true, true, { Nullable and (ListAssociation and EntityAssociation).not() }),
    NotNull("notNull", true, true, { (ListAssociation and EntityAssociation).not() }),
    ValueIn("valueIn", true, true, { EntityAssociation.not() }),
    ValueNotIn("valueNotIn", true, false, { EntityAssociation.not() }),
    AssociatedIdEq("associatedIdEq", true, true, { EntityAssociation }),
    AssociatedIdNe("associatedIdNe", true, false, { EntityAssociation }),
    AssociatedIdIn("associatedIdIn", true, true, { EntityAssociation }),
    AssociatedIdNotIn("associatedIdNotIn", true, false, { EntityAssociation }),
}