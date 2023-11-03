package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.structure.Property

enum class ArgType(val test: (Property) -> Boolean) {
    // 任意属性
    Prop({ true }),

    // 关联
    Scalar({ property -> property.simpleAnnotations.none { annotation -> annotation in RelationType.values().map { it.name } } }),
    Association({ property -> property.simpleAnnotations.any { annotation -> annotation in RelationType.values().map { it.name } } }),
    SingleAssociation({ property -> property.simpleAnnotations.any { annotation -> annotation in RelationType.singles().map { it.name } } }),
    ListAssociation({ property -> property.simpleAnnotations.any { annotation -> annotation in RelationType.lists().map { it.name } } }),

    // 字符串
    String({ it.type == "String" }),

    // nullity
    Null({ it.nullable }),

    // 特性属性
    Formula({ "Formula" in it.simpleAnnotations }), Id({ "Id" in it.simpleAnnotations }), Key({ "Key" in it.simpleAnnotations })
}