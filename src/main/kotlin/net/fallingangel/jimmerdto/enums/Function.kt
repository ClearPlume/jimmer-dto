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
    val whetherBody: Boolean,
    val whetherSpec: Boolean,
    val whetherMultiArg: Boolean,
    val argConstraint: ((DTODto) -> ArgType)? = null,
) {
    Id("id", false, false, false, { EntityAssociation }),
    Flat("flat", true, false, false, {
        if (it modifiedBy Modifier.Specification) {
            EntityAssociation or Embeddable or ListAssociation
        } else {
            (EntityAssociation or Embeddable) and ListAssociation.not()
        }
    }),
    Fold("fold", true, false, false),
    Eq("eq", false, true, true, { EntityAssociation.not() }),
    Ne("ne", false, true, false, { EntityAssociation.not() }),
    Gt("gt", false, true, false, { EntityAssociation.not() }),
    Ge("ge", false, true, false, { EntityAssociation.not() }),
    Lt("lt", false, true, false, { EntityAssociation.not() }),
    Le("le", false, true, false, { EntityAssociation.not() }),
    Like("like", false, true, true, { StringProp }),
    NotLike("notLike", false, true, false, { StringProp }),
    Null("null", false, true, true, { Nullable and (ListAssociation and EntityAssociation).not() }),
    NotNull("notNull", false, true, true, { (ListAssociation and EntityAssociation).not() }),
    ValueIn("valueIn", false, true, true, { EntityAssociation.not() }),
    ValueNotIn("valueNotIn", false, true, false, { EntityAssociation.not() }),
    AssociatedIdEq("associatedIdEq", false, true, true, { EntityAssociation }),
    AssociatedIdNe("associatedIdNe", false, true, false, { EntityAssociation }),
    AssociatedIdIn("associatedIdIn", false, true, true, { EntityAssociation }),
    AssociatedIdNotIn("associatedIdNotIn", false, true, false, { EntityAssociation }),
}