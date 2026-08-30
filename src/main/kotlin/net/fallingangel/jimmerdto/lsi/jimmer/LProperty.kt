@file:Suppress("LanguageDetectionInspection")

package net.fallingangel.jimmerdto.lsi.jimmer

import net.fallingangel.jimmerdto.enums.SimplePropType
import net.fallingangel.jimmerdto.enums.SimplePropType.BigDecimal
import net.fallingangel.jimmerdto.enums.SimplePropType.BigInteger
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Embeddable
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Entity
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Formula
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.GeneratedValue
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Id
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.IdView
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Immutable
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Key
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.MappedSuperclass
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations.Transient
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp

val LProperty.Type.resolvedLClass: LClass?
    get() = (this as? LProperty.Type.Clazz)?.clazz

/**
 * 目标类型标注了 [Embeddable]——值对象，字段内嵌在宿主表里，没有外键和独立 id。
 * 结构上像关联（有嵌套属性），语义上不是。
 */
val LProperty.isEmbedded: Boolean
    get() = targetClass?.hasAnnotation(Embeddable) == true

/**
 * 目标类型标注了 [Entity]——真正的关联：有外键，有独立表，有 id。
 * 等价于编译器的 isAssociation(true)。
 *
 * 编译器另有 isAssociation(false)，即 isImmutable：目标类型标注了
 * [Entity]/[MappedSuperclass]/[Embeddable] 三者之一（互斥），
 * 或者单独标注了 [Immutable]（[Immutable] 可与任一 SQL 注解共存，
 * 但共存时以 SQL 注解为准）。
 * 在插件语境里 [MappedSuperclass] 不出现在属性类型位置，
 * [Immutable] 不出现在 SQL 实体中，实际命中的只有 [Entity] 和 [Embeddable]，
 * 所以不需要这个并集——每个场景直接用 isEntityAssociation 或 isEmbedded。
 */
val LProperty.isEntityAssociation: Boolean
    get() = targetClass?.hasAnnotation(Entity) == true

val LProperty.isFormula: Boolean
    get() = hasAnnotation(Formula)

val LProperty.isId: Boolean
    get() = hasAnnotation(Id)

val LProperty.isKey: Boolean
    get() = hasAnnotation(Key)

val LProperty.isRecursive: Boolean
    get() = isEntityAssociation && containingLClass == targetClass

val LProperty.isList: Boolean
    get() = type is LProperty.Type.Collection

val LProperty.isTransient: Boolean
    get() = hasAnnotation(Transient)

/**
 * 单引用实体关联——实体关联、非集合、非 transient。
 * 等价于编译器的 isAutoReference 判断。
 */
val LProperty.isReference: Boolean
    get() = isEntityAssociation && !isList && !isTransient

val LClass.idProperty: LProperty?
    get() = properties.find { it.isId }

val LProperty.idViewBaseProp: LProperty?
    get() {
        val annotation = findAnnotation(IdView) ?: return null
        val baseParam = annotation.findParam("value") ?: return null
        val declaredBase = (baseParam.value ?: baseParam.defaultValue)
            ?.let { it as? LAnnotation.Param.Value.Scalar }
            ?.value as? String
        val base = declaredBase?.takeIf { it.isNotEmpty() } ?: defaultViewBasePropName ?: return null
        if (base == name) return null

        return containingLClass.findProperty(base)?.takeIf { it.isReference }
    }

val LProperty.defaultViewBasePropName: String?
    get() = defaultViewBasePropName(name, isList)

fun defaultViewBasePropName(name: String, isList: Boolean): String? {
    if (isList) return null
    if (name.length <= 2) return null
    if (!name.endsWith("Id")) return null
    if (name[name.length - 3].isUpperCase()) return null
    return name.dropLast(2)
}

val LProperty.isGeneratedValue: Boolean
    get() = hasAnnotation(GeneratedValue)

private val SIMPLE_PROP_TYPE_MAP = mapOf(
    "boolean" to SimplePropType.Boolean, "java.lang.Boolean" to SimplePropType.Boolean, "kotlin.Boolean" to SimplePropType.Boolean,
    "byte" to SimplePropType.Byte, "java.lang.Byte" to SimplePropType.Byte, "kotlin.Byte" to SimplePropType.Byte,
    "short" to SimplePropType.Short, "java.lang.Short" to SimplePropType.Short, "kotlin.Short" to SimplePropType.Short,
    "int" to SimplePropType.Int, "java.lang.Integer" to SimplePropType.Int, "kotlin.Int" to SimplePropType.Int,
    "long" to SimplePropType.Long, "java.lang.Long" to SimplePropType.Long, "kotlin.Long" to SimplePropType.Long,
    "float" to SimplePropType.Float, "java.lang.Float" to SimplePropType.Float, "kotlin.Float" to SimplePropType.Float,
    "double" to SimplePropType.Double, "java.lang.Double" to SimplePropType.Double, "kotlin.Double" to SimplePropType.Double,

    "java.math.BigInteger" to BigInteger,
    "java.math.BigDecimal" to BigDecimal,

    "java.lang.String" to SimplePropType.String, "kotlin.String" to SimplePropType.String,
)

val LProperty.simplePropType: SimplePropType?
    get() = (type as? LProperty.Type.Scalar)
        ?.name
        ?.let(SIMPLE_PROP_TYPE_MAP::get)

// TODO 多参函数
val DTOPositiveProp.baseProperty: LProperty?
    get() {
        val arg = arg
        return if (arg != null) arg.values.firstOrNull()?.property else property
    }
