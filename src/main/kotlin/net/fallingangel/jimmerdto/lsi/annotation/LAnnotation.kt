package net.fallingangel.jimmerdto.lsi.annotation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.lsi.LDependencyProvider
import net.fallingangel.jimmerdto.lsi.LElement
import net.fallingangel.jimmerdto.lsi.LName
import net.fallingangel.jimmerdto.lsi.process

class LAnnotation(
    lName: LName,
    val params: List<Param>,
    override val dependencyItem: PsiElement,
) : LElement, LDependencyProvider {
    override val name = lName.name
    val fqName = lName.fqName

    class Param(
        override val name: String,
        val type: Type,
        val value: Value?,
        val defaultValue: Value?,
        override val dependencyItem: PsiNamedElement,
    ) : LElement, LDependencyProvider {
        sealed class Type : LDependencyProvider {
            abstract val presentation: String

            open fun accepts(value: Value): Boolean {
                return when (this) {
                    is Scalar -> value is Value.Scalar && accepts(value)
                    is Enum -> value is Value.Enum && fqName == value.typeName
                    is Clazz -> value is Value.Clazz && accepts(value)
                    is Annotation -> value is Value.Annotation && fqName == value.annotation.fqName
                    is Array -> when (value) {
                        is Value.Array -> value.elements.filterNotNull().all { elementType.accepts(it) }
                        else -> elementType.accepts(value)
                    }
                }
            }

            override fun toString(): String {
                return toDebugString(mutableSetOf())
            }

            fun toDebugString(visited: MutableSet<String>): String = when (this) {
                is Scalar -> presentation
                is Enum -> presentation
                is Array -> "[${elementType.toDebugString(visited)}]"
                is Clazz -> presentation
                is Annotation -> presentation
            }

            class Scalar(val kind: Kind) : Type() {
                override val presentation = kind.toString()

                enum class Kind {
                    Boolean, Byte, Short, Int, Long, Float, Double, Char, String
                }

                override fun accepts(value: Value): Boolean {
                    if (value !is Value.Scalar) return false
                    val v = value.value
                    return when (kind) {
                        Kind.Boolean -> v is Boolean
                        Kind.Char -> v is Char
                        Kind.String -> v is String
                        Kind.Byte -> v is Byte || (v is Int && v in Byte.MIN_VALUE..Byte.MAX_VALUE)
                        Kind.Short -> v is Short || (v is Int && v in Short.MIN_VALUE..Short.MAX_VALUE)
                        Kind.Int -> v is Int
                        Kind.Long -> v is Long || v is Int
                        Kind.Float -> v is Float || v is Int || (v is Double && v in -Float.MAX_VALUE..Float.MAX_VALUE)
                        Kind.Double -> v is Double || v is Float || v is Int
                    }
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Scalar

                    return kind == other.kind
                }

                override fun hashCode(): Int {
                    return kind.hashCode()
                }
            }

            @Suppress("StatefulEp")
            // false positive: not an EP, lifecycle bound to CachedValue
            class Enum(lName: LName, entries: List<Pair<String, PsiElement>>, override val dependencyItem: Any) : Type() {
                override val presentation = lName.fqName
                val name = lName.name
                val fqName = lName.fqName

                val constants = entries.toMap()

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Enum

                    return fqName == other.fqName
                }

                override fun hashCode(): Int {
                    return fqName.hashCode()
                }
            }

            class Array(val elementType: Type) : Type() {
                override val presentation = "Array<${elementType.presentation}>"

                override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
                    elementType.collectDependencyItems(result, visited)
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Array

                    return elementType == other.elementType
                }

                override fun hashCode(): Int {
                    return elementType.hashCode()
                }
            }

            class Clazz(val bound: LName?, override val dependencyItem: PsiElement) : Type() {
                override val presentation = if (bound != null) "Class<${bound.name}>" else "Class<?>"

                override fun accepts(value: Value): Boolean {
                    if (value !is Value.Clazz) return false
                    val bound = bound?.fqName ?: return true
                    return process(dependencyItem) { isInheritorOrSelf(value.typeName, bound) } ?: true
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Clazz

                    return bound == other.bound
                }

                override fun hashCode(): Int {
                    return bound.hashCode()
                }
            }

            class Annotation(lName: LName, override val dependencyItem: Any) : Type() {
                override val presentation = lName.name
                val fqName = lName.fqName
                val name = lName.name

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Annotation

                    return fqName == other.fqName
                }

                override fun hashCode(): Int {
                    return fqName.hashCode()
                }
            }
        }

        sealed class Value {
            abstract val typeName: String
            abstract val presentation: String

            override fun toString(): String {
                return presentation
            }

            class Scalar(val value: Any) : Value() {
                override val typeName = when (value) {
                    is Boolean -> "Boolean"
                    is Byte -> "Byte"
                    is Short -> "Short"
                    is Int -> "Int"
                    is Long -> "Long"
                    is Float -> "Float"
                    is Double -> "Double"
                    is Char -> "Char"
                    is String -> "String"
                    else -> value::class.simpleName ?: "?"
                }
                override val presentation = value.toString()

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Scalar

                    return value == other.value
                }

                override fun hashCode(): Int {
                    return value.hashCode()
                }
            }

            class Enum(lName: LName, val constantName: String) : Value() {
                override val typeName = lName.fqName
                override val presentation = "$typeName.$constantName"

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Enum

                    if (typeName != other.typeName) return false
                    if (constantName != other.constantName) return false

                    return true
                }

                override fun hashCode(): Int {
                    var result = typeName.hashCode()
                    result = 31 * result + constantName.hashCode()
                    return result
                }
            }

            class Array(val elements: List<Value?>) : Value() {
                override val typeName = elements.filterNotNull().firstOrNull()?.typeName?.plus("[]") ?: "Array"
                override val presentation = elements.joinToString(separator = ", ", prefix = "[", postfix = "]") { it?.presentation ?: "?" }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Array

                    return elements == other.elements
                }

                override fun hashCode(): Int {
                    return elements.hashCode()
                }
            }

            class Clazz(lName: LName) : Value() {
                override val typeName = lName.fqName
                override val presentation = "Class<$typeName>"

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Clazz

                    return typeName == other.typeName
                }

                override fun hashCode(): Int {
                    return typeName.hashCode()
                }
            }

            class Annotation(val annotation: LAnnotation) : Value() {
                override val typeName = annotation.fqName
                override val presentation = annotation.toString()

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Annotation

                    return annotation == other.annotation
                }

                override fun hashCode(): Int {
                    return annotation.hashCode()
                }
            }
        }

        fun accepts(value: Value): Boolean {
            return type.accepts(value)
        }

        override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
            type.collectDependencyItems(result, visited)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Param

            if (name != other.name) return false
            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        override fun toString(): String {
            return toDebugString(mutableSetOf())
        }

        fun toDebugString(visited: MutableSet<String>): String {
            return "Param(name=$name, type=${type.toDebugString(visited)}, value=$value, defaultValue=$defaultValue)"
        }
    }

    override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
        params.forEach { it.collectDependencyItems(result, visited) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LAnnotation

        if (fqName != other.fqName) return false
        if (params.map(Param::value) != other.params.map(Param::value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + params.map(Param::value).hashCode()
        return result
    }

    override fun toString(): String {
        return toDebugString(mutableSetOf())
    }

    fun toDebugString(visited: MutableSet<String>): String {
        val paramsStr = paramsToString(visited)
        return "@LAnnotation(name=$name, params=$paramsStr)"
    }

    fun paramsToString(visited: MutableSet<String>): String {
        return params.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
    }
}
