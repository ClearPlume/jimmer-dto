package net.fallingangel.jimmerdto.structure

import kotlin.reflect.full.createInstance

enum class BasicType {
    Boolean, Char, String, Byte, Short, Int, Float, Double, Long, Any;

    companion object {
        fun types(): List<String> {
            val types = entries.map { it.name }
            return types + types.map { "$it?" }
        }
    }
}

/**
 * @param generics 类型泛型列表
 */
@Suppress("unused")
sealed class GenericType(vararg val generics: String) {
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

        operator fun get(type: String): GenericType? {
            return GenericType::class.sealedSubclasses.find { it.simpleName == type }?.createInstance()
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