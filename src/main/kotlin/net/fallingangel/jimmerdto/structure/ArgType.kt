package net.fallingangel.jimmerdto.structure

import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.jimmer.isEmbedded
import net.fallingangel.jimmerdto.lsi.jimmer.isEntityAssociation
import net.fallingangel.jimmerdto.lsi.jimmer.isList

sealed class ArgType(val test: (LProperty) -> Boolean) {
    companion object {
        infix fun ArgType.or(other: ArgType) = Or(this, other)
        infix fun ArgType.and(other: ArgType) = And(this, other)
        fun ArgType.not() = Not(this)
    }

    class And(vararg argTypes: ArgType) : ArgType({ property -> argTypes.all { it.test(property) } })
    class Or(vararg argTypes: ArgType) : ArgType({ property -> argTypes.any { it.test(property) } })
    class Not(argType: ArgType) : ArgType({ property -> !argType.test(property) })

    object EntityAssociation : ArgType(LProperty::isEntityAssociation)
    object Embeddable : ArgType(LProperty::isEmbedded)
    object ListAssociation : ArgType(LProperty::isList)
    object StringProp : ArgType({ it.type is LProperty.Type.Scalar && (it.type.name == "java.lang.String" || it.type.name == "kotlin.String") })

    object Nullable : ArgType(LProperty::nullable)
}
