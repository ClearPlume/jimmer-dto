package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.lsi.annotation.*
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations

class LProperty(
    override val name: String,
    val type: Type,
    val abstract: Boolean,
    override val annotations: List<LAnnotation>,
    override val dependencyItem: PsiNamedElement,
    val containingLClass: LClass,
) : LElement, LAnnotationOwner, LDependencyProvider {
    sealed class Type : LDependencyProvider {
        abstract val nullable: Boolean

        /**
         * 集合是关联载体，数组是标量数据(byte[])
         */
        abstract val actualType: Type

        abstract val presentation: String

        override fun toString(): String {
            return toDebugString(mutableSetOf())
        }

        fun toDebugString(visited: MutableSet<String>): String = when (this) {
            is Scalar -> "Scalar(name=$presentation, nullable=$nullable)"
            is Enum -> "Enum(name=$presentation, fqName=$fqName, nullable=$nullable, values=$constants)"
            is Array -> "Array(nullable=$nullable, elementType=${elementType.toDebugString(visited)})"
            is Collection -> "Collection(nullable=$nullable, kind=$kind, elementType=${elementType.toDebugString(visited)})"
            is Map -> "Map(nullable=$nullable, keyType=${keyType.toDebugString(visited)}, valueType=${valueType.toDebugString(visited)})"
            is Clazz -> "Class(nullable=$nullable, class=${clazz.toDebugString(visited)})"
        }

        class Scalar(val name: String, override val nullable: Boolean) : Type() {
            override val actualType = this
            override val presentation = name

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Scalar

                return name == other.name
            }

            override fun hashCode(): Int {
                return name.hashCode()
            }
        }

        @Suppress("StatefulEp")
        // false positive: not an EP, lifecycle bound to CachedValue
        class Enum(
            lName: LName,
            entries: List<Pair<String, PsiElement>>,
            override val nullable: Boolean,
            override val dependencyItem: Any,
        ) : Type() {
            override val actualType = this
            override val presentation = lName.name
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

        class Array(val elementType: Type, override val nullable: Boolean) : Type() {
            override val actualType = this
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

        class Collection(val elementType: Type, val kind: Kind, override val nullable: Boolean) : Type() {
            override val actualType = elementType
            override val presentation = "$kind<${elementType.presentation}>"

            enum class Kind {
                List, Set, Queue
            }

            override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
                elementType.collectDependencyItems(result, visited)
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Collection

                if (elementType != other.elementType) return false
                if (kind != other.kind) return false

                return true
            }

            override fun hashCode(): Int {
                var result = elementType.hashCode()
                result = 31 * result + kind.hashCode()
                return result
            }
        }

        class Map(val keyType: Type, val valueType: Type, override val nullable: Boolean) : Type() {
            override val actualType = this
            override val presentation = "Map<${keyType.presentation}, ${valueType.presentation}>"

            override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
                keyType.collectDependencyItems(result, visited)
                valueType.collectDependencyItems(result, visited)
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Map

                if (keyType != other.keyType) return false
                if (valueType != other.valueType) return false

                return true
            }

            override fun hashCode(): Int {
                var result = keyType.hashCode()
                result = 31 * result + valueType.hashCode()
                return result
            }
        }

        class Clazz(val clazz: LClass, override val nullable: Boolean, override val dependencyItem: Any) : Type() {
            override val actualType = this
            override val presentation = clazz.name

            override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
                clazz.collectDependencyItems(result, visited)
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Clazz

                return clazz == other.clazz
            }

            override fun hashCode(): Int {
                return clazz.hashCode()
            }
        }
    }

    val nullable = hasAnnotationBySimple("Null", "Nullable")
            || hasAnnotation(JimmerAnnotations.TNullable)
            || type.nullable

    val actualType = type.actualType

    val targetClass: LClass?
        get() = (actualType as? Type.Clazz)?.clazz

    val presentableType = buildString {
        append(type.presentation)
        if (nullable) {
            append("?")
        }
    }

    override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
        annotations.forEach { it.collectDependencyItems(result, visited) }
        type.collectDependencyItems(result, visited)
    }

    override fun toString(): String {
        return toDebugString(mutableSetOf())
    }

    fun toDebugString(visited: MutableSet<String>): String {
        val annotationsStr = annotationsToString(visited)
        return "LProperty(name=$name, type=${type.toDebugString(visited)}, abstract=$abstract, annotations=$annotationsStr)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LProperty

        if (name != other.name) return false
        if (containingLClass.fqName != other.containingLClass.fqName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + containingLClass.fqName.hashCode()
        return result
    }
}
