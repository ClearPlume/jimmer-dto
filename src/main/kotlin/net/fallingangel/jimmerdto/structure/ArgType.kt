package net.fallingangel.jimmerdto.structure

import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.hasExactlyOneAnnotation
import org.babyfish.jimmer.sql.*

sealed class ArgType(val test: (LProperty<*>) -> Boolean) {
    companion object {
        infix fun ArgType.or(other: ArgType) = Or(this, other)
        infix fun ArgType.and(other: ArgType) = And(this, other)
    }

    class And(vararg argTypes: ArgType) : ArgType({ property -> argTypes.all { it.test(property) } })
    class Or(vararg argTypes: ArgType) : ArgType({ property -> argTypes.any { it.test(property) } })

    // 任意属性
    object Prop : ArgType({ true })

    // 关联
    object Scalar : ArgType({ property -> !property.isAssociation })

    object Embeddable : ArgType(({ it.doesTypeHaveAnnotation(org.babyfish.jimmer.sql.Embeddable::class) }))

    object Association : ArgType(LProperty<*>::isAssociation)

    object SingleAssociation : ArgType({ it.hasExactlyOneAnnotation(OneToOne::class, ManyToOne::class) })

    object ListAssociation : ArgType({ it.hasExactlyOneAnnotation(OneToMany::class, ManyToMany::class, ManyToManyView::class) })

    // 字符串
    object String : ArgType({ it.type.name == "String" || it.type.name == "String?" })

    // nullity
    object Null : ArgType({ it.nullable })
    object NotNull : ArgType({ !it.nullable })

    // 特性属性
    object Formula : ArgType({ it.hasAnnotation(org.babyfish.jimmer.Formula::class) })
    object Id : ArgType({ it.hasAnnotation(org.babyfish.jimmer.sql.Id::class) })
    object Key : ArgType({ it.hasAnnotation(org.babyfish.jimmer.sql.Key::class) })
}