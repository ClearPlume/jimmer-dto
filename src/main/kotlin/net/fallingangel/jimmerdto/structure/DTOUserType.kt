package net.fallingangel.jimmerdto.structure

import kotlin.reflect.full.createInstance

enum class BasicType {
    Boolean, Char, String, Byte, Short, Int, Float, Double, Long, Any;

    companion object {
        fun types(): List<kotlin.String> {
            val types = BasicType.values().map { it.name }
            return types + types.map { "$it?" }
        }
    }
}

/**
 * @param generics 类型泛型列表
 */
@Suppress("unused")
sealed class GenericType(private vararg val generics: String) {
    private val name: String = this::class.simpleName!!

    private val tail: String = generics.joinToString(prefix = "<", postfix = ">")

    open val lookup: LookupInfo
        get() = LookupInfo(
            name,
            "$name<>",
            "$name$tail",
            tail,
            -1
        )

    companion object {
        fun types(): kotlin.collections.List<LookupInfo> {
            return GenericType::class.sealedSubclasses.map { it.createInstance().lookup }
        }
    }

    class Array : GenericType("T")
    class List : GenericType("E")
    class MutableList : GenericType("E")
    class Collection : GenericType("E")
    class MutableCollection : GenericType("E")
    class Iterable : GenericType("E")
    class MutableIterable : GenericType("E")
    class Set : GenericType("E")
    class MutableSet : GenericType("E")
    class Map : GenericType("K", "V")
    class MutableMap : GenericType("K", "V")
}