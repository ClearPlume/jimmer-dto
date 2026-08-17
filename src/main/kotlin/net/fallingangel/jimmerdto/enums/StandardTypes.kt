package net.fallingangel.jimmerdto.enums

enum class StandardType(vararg typeParams: String) {
    Boolean, Char, Byte, Short, Int, Long, Float, Double, Any, String,
    Array("T"),
    Iterable("E"), MutableIterable("E"),
    Collection("E"), MutableCollection("E"),
    List("E"), MutableList("E"),
    Set("E"), MutableSet("E"),
    Map("K", "V"), MutableMap("K", "V");

    val arity = typeParams.size

    companion object {
        private val byName = entries.associateBy { it.name }
        operator fun get(name: kotlin.String) = byName[name]
    }
}

val AUTO_IMPORTED_TYPES = setOf(
    "void", "boolean", "char", "byte", "short", "int", "long", "float", "double",

    "java.lang.Boolean", "java.lang.Character", "java.lang.Void",
    "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
    "java.lang.Long", "java.lang.Float", "java.lang.Double",
    "java.lang.Object", "java.lang.String", "java.lang.Iterable",
    "java.util.Collection", "java.util.List", "java.util.Set", "java.util.Map",

    "kotlin.Unit", "kotlin.Boolean", "kotlin.Char", "kotlin.Byte", "kotlin.Short",
    "kotlin.Int", "kotlin.Long", "kotlin.Float", "kotlin.Double",
    "kotlin.Any", "kotlin.String",
    "kotlin.Array", "kotlin.BooleanArray", "kotlin.CharArray", "kotlin.ByteArray",
    "kotlin.ShortArray", "kotlin.IntArray", "kotlin.LongArray",
    "kotlin.FloatArray", "kotlin.DoubleArray",

    "kotlin.collections.Iterable", "kotlin.collections.Collection",
    "kotlin.collections.List", "kotlin.collections.Set", "kotlin.collections.Map",
    "kotlin.collections.MutableIterable", "kotlin.collections.MutableCollection",
    "kotlin.collections.MutableList", "kotlin.collections.MutableSet",
    "kotlin.collections.MutableMap",
)

private typealias S = StandardType

/**
 * 标准类型的一个具体写法。[args] 与 [StandardType.arity] 不强制一致
 * 不一致是 Annotator 的报错点，不是构造期的异常
 */
data class StandardTypeRef(
    val type: S,
    val args: List<S> = emptyList(),
    val nullable: Boolean = false,
)

private val S.only get() = listOf(StandardTypeRef(this))
private val S.nullable get() = listOf(StandardTypeRef(this, nullable = true))
private fun S.of(vararg args: S) = listOf(StandardTypeRef(this, args.toList()))
private infix fun S.or(other: S) = listOf(StandardTypeRef(this), StandardTypeRef(other))

/**
 * 非法类型名到应当改写成的标准类型写法。
 *
 * 与 [StandardType] 共同构成保留名字空间，两表不相交：
 * [StandardType] 是认的名字，本表是看起来像但写法不对的名字。
 * 编译器侧对应 `ILLEGAL_TYPES`，其值为单个建议；
 * 此处为 List 是因为可变性歧义（`java.util.List` 既可能是 `List` 也可能是 `MutableList`），
 * 编译器只能任选其一，插件可给出全部候选作为 quick-fix。
 * 错误文案仍以编译器的单个建议为准，候选不进消息文本。
 */
val ILLEGAL_TYPES: Map<String, List<StandardTypeRef>> = mapOf(
    "boolean" to S.Boolean.only,
    "java.lang.Boolean" to S.Boolean.nullable,
    "kotlin.Boolean" to S.Boolean.only,

    "char" to S.Char.only,
    "java.lang.Character" to S.Char.nullable,
    "Character" to S.Char.nullable,
    "kotlin.Char" to S.Char.only,

    "byte" to S.Byte.only,
    "java.lang.Byte" to S.Byte.nullable,
    "kotlin.Byte" to S.Byte.only,

    "short" to S.Short.only,
    "java.lang.Short" to S.Short.nullable,
    "kotlin.Short" to S.Short.only,

    "int" to S.Int.only,
    "java.lang.Integer" to S.Int.nullable,
    "Integer" to S.Int.nullable,
    "kotlin.Int" to S.Int.only,

    "long" to S.Long.only,
    "java.lang.Long" to S.Long.nullable,
    "kotlin.Long" to S.Long.only,

    "float" to S.Float.only,
    "java.lang.Float" to S.Float.nullable,
    "kotlin.Float" to S.Float.only,

    "double" to S.Double.only,
    "java.lang.Double" to S.Double.nullable,
    "kotlin.Double" to S.Double.only,

    "string" to S.String.only,
    "java.lang.String" to S.String.only,
    "kotlin.String" to S.String.only,

    "kotlin.Array" to S.Array.only,
    "kotlin.BooleanArray" to S.Array.of(S.Boolean),
    "kotlin.CharArray" to S.Array.of(S.Char),
    "kotlin.ByteArray" to S.Array.of(S.Byte),
    "kotlin.ShortArray" to S.Array.of(S.Short),
    "kotlin.IntArray" to S.Array.of(S.Int),
    "kotlin.LongArray" to S.Array.of(S.Long),
    "kotlin.FloatArray" to S.Array.of(S.Float),
    "kotlin.DoubleArray" to S.Array.of(S.Double),

    "java.lang.Iterable" to (S.Iterable or S.MutableIterable),
    "kotlin.collections.Iterable" to S.Iterable.only,
    "kotlin.collections.MutableIterable" to S.MutableIterable.only,

    "java.util.Collection" to (S.Collection or S.MutableCollection),
    "kotlin.collections.Collection" to S.Collection.only,
    "kotlin.collections.MutableCollection" to S.MutableCollection.only,

    "java.util.List" to (S.List or S.MutableList),
    "kotlin.collections.List" to S.List.only,
    "kotlin.collections.MutableList" to S.MutableList.only,

    "java.util.Set" to (S.Set or S.MutableSet),
    "kotlin.collections.Set" to S.Set.only,
    "kotlin.collections.MutableSet" to S.MutableSet.only,

    "java.util.Map" to (S.Map or S.MutableMap),
    "kotlin.collections.Map" to S.Map.only,
    "kotlin.collections.MutableMap" to S.MutableMap.only,
)
