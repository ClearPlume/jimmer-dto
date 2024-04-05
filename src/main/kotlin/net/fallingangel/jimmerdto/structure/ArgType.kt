package net.fallingangel.jimmerdto.structure

import net.fallingangel.jimmerdto.enums.RelationType

sealed class ArgType(val test: (Property) -> Boolean) {
    companion object {
        infix fun ArgType.or(other: ArgType) = Or(this, other)
        infix fun ArgType.and(other: ArgType) = And(this, other)
    }

    class And(vararg argTypes: ArgType) : ArgType({ property -> argTypes.all { it.test(property) } })
    class Or(vararg argTypes: ArgType) : ArgType({ property -> argTypes.any { it.test(property) } })

    // 任意属性
    object Prop : ArgType({ true })

    // 关联
    object Scalar : ArgType({ property ->
        property.simpleAnnotations.none { annotation -> annotation in RelationType.all }
    })

    object Association : ArgType({ property ->
        property.simpleAnnotations.any { annotation -> annotation in RelationType.all }
    })

    object SingleAssociation : ArgType({ property ->
        property.simpleAnnotations.any { annotation -> annotation in RelationType.singles }
    })

    object ListAssociation : ArgType({ property ->
        property.simpleAnnotations.any { annotation -> annotation in RelationType.lists }
    })

    // 字符串
    object String : ArgType({ it.type == "String" || it.type == "String?" })

    // nullity
    object Null : ArgType({ it.nullable })
    object NotNull : ArgType({ !it.nullable })

    // 特性属性
    object Formula : ArgType({ "Formula" in it.simpleAnnotations })
    object Id : ArgType({ "Id" in it.simpleAnnotations })
    object Key : ArgType({ "Key" in it.simpleAnnotations })
}