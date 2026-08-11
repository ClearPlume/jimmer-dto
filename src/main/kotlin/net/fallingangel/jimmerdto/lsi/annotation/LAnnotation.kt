package net.fallingangel.jimmerdto.lsi.annotation

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LElement
import net.fallingangel.jimmerdto.lsi.LPsiDependent
import net.fallingangel.jimmerdto.lsi.process

class LAnnotation(
    override val name: String,
    val canonicalName: String,
    val params: List<Param>,
    override val source: PsiElement?,
) : LElement, LPsiDependent {
    class Param(
        override val name: String,
        val type: Type,
        val value: Value?,
        val defaultValue: Value?,
        override val source: PsiElement?,
    ) : LElement, LPsiDependent {
        sealed class Type : LPsiDependent {
            abstract val presentation: String

            open fun accepts(value: Value): Boolean {
                return when (this) {
                    is Scalar -> value is Value.Scalar && accepts(value)
                    is Enum -> value is Value.Enum && canonicalName == value.canonicalName
                    is Clazz -> value is Value.Clazz && accepts(value)
                    is Annotation -> value is Value.Annotation && canonicalName == value.annotation.canonicalName
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
                override val source = null

                enum class Kind {
                    BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR, STRING
                }

                override fun accepts(value: Value): Boolean {
                    if (value !is Value.Scalar) return false
                    val v = value.value
                    return when (kind) {
                        Kind.BOOLEAN -> v is Boolean
                        Kind.CHAR -> v is Char
                        Kind.STRING -> v is String
                        Kind.BYTE -> v is Byte || (v is Int && v in Byte.MIN_VALUE..Byte.MAX_VALUE)
                        Kind.SHORT -> v is Short || (v is Int && v in Short.MIN_VALUE..Short.MAX_VALUE)
                        Kind.INT -> v is Int
                        Kind.LONG -> v is Long || v is Int
                        Kind.FLOAT -> v is Float || v is Int || (v is Double && v in -Float.MAX_VALUE..Float.MAX_VALUE)
                        Kind.DOUBLE -> v is Double || v is Float || v is Int
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
            class Enum(val canonicalName: String, entries: List<Pair<String, PsiElement>>, override val source: PsiElement?) : Type() {
                override val presentation = canonicalName

                val constants = entries.toMap()

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Enum

                    return canonicalName == other.canonicalName
                }

                override fun hashCode(): Int {
                    return canonicalName.hashCode()
                }
            }

            class Array(val elementType: Type) : Type() {
                override val presentation = "Array<${elementType.presentation}>"
                override val source = null

                override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
                    elementType.collectPsiElements(result, visited)
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

            class Clazz(val bound: String?, override val source: PsiElement?) : Type() {
                override val presentation = if (bound != null) "Class<$bound>" else "Class<?>"

                override fun accepts(value: Value): Boolean {
                    if (value !is Value.Clazz) return false
                    val bound = bound ?: return true
                    val source = source ?: return true
                    return process(source) { isInheritorOrSelf(value.canonicalName, bound) } ?: true
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

            @Suppress("StatefulEp")
            class Annotation(val canonicalName: String, override val source: PsiElement?) : Type() {
                override val presentation = canonicalName

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Annotation

                    return canonicalName == other.canonicalName
                }

                override fun hashCode(): Int {
                    return canonicalName.hashCode()
                }
            }
        }

        sealed class Value {
            abstract val presentation: String

            override fun toString(): String {
                return presentation
            }

            class Scalar(val value: Any) : Value() {
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

            class Enum(val canonicalName: String, val constantName: String) : Value() {
                override val presentation = "$canonicalName.$constantName"

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Enum

                    if (canonicalName != other.canonicalName) return false
                    if (constantName != other.constantName) return false

                    return true
                }

                override fun hashCode(): Int {
                    var result = canonicalName.hashCode()
                    result = 31 * result + constantName.hashCode()
                    return result
                }
            }

            class Array(val elements: List<Value?>) : Value() {
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

            class Clazz(val canonicalName: String) : Value() {
                override val presentation = "Class<$canonicalName>"
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Clazz

                    return canonicalName == other.canonicalName
                }

                override fun hashCode(): Int {
                    return canonicalName.hashCode()
                }
            }

            class Annotation(val annotation: LAnnotation) : Value() {
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

        override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
            type.collectPsiElements(result, visited)
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

    override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        params.forEach { it.collectPsiElements(result, visited) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LAnnotation

        if (canonicalName != other.canonicalName) return false
        if (params.map(Param::value) != other.params.map(Param::value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = canonicalName.hashCode()
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