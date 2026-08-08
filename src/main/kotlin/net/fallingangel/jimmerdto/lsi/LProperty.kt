package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.*
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations

class LProperty(
    override val name: String,
    val type: Type,
    val abstract: Boolean,
    override val annotations: List<LAnnotation>,
    override val source: PsiElement?,
    val containingLClass: LClass,
) : LElement, LAnnotationOwner, LPsiDependent {
    sealed class Type : LPsiDependent {
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
            is Enum -> "Enum(name=$presentation, canonicalName=$canonicalName, nullable=$nullable, values=$constants)"
            is Array -> "Array(nullable=$nullable, elementType=${elementType.toDebugString(visited)})"
            is Collection -> "Collection(nullable=$nullable, kind=$kind, elementType=${elementType.toDebugString(visited)})"
            is Map -> "Map(nullable=$nullable, keyType=${keyType.toDebugString(visited)}, valueType=${valueType.toDebugString(visited)})"
            is Clazz -> "Class(nullable=$nullable, class=${clazz.toDebugString(visited)})"
        }

        class Scalar(val canonicalName: String, override val nullable: Boolean) : Type() {
            override val actualType = this
            override val presentation = canonicalName.substringAfterLast('.')
            override val source = null

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Scalar

                return canonicalName == other.canonicalName
            }

            override fun hashCode(): Int {
                return canonicalName.hashCode()
            }
        }

        @Suppress("StatefulEp")
        // false positive: not an EP, lifecycle bound to CachedValue
        class Enum(
            val canonicalName: String,
            entries: List<Pair<String, PsiElement>>,
            override val nullable: Boolean,
            override val source: PsiElement?,
        ) : Type() {
            override val actualType = this
            override val presentation = canonicalName

            val constants = entries.toMap()

            override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
                result.addAll(constants.values)
            }

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

        class Array(val elementType: Type, override val nullable: Boolean) : Type() {
            override val actualType = this
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

        class Collection(val elementType: Type, val kind: Kind, override val nullable: Boolean) : Type() {
            override val actualType = elementType
            override val presentation = "$kind<${elementType.presentation}>"
            override val source = null

            enum class Kind {
                List, Set, Queue
            }

            override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
                elementType.collectPsiElements(result, visited)
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
            override val source = null

            override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
                keyType.collectPsiElements(result, visited)
                valueType.collectPsiElements(result, visited)
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

        class Clazz(val clazz: LClass, override val nullable: Boolean, override val source: PsiElement?) : Type() {
            override val actualType = this
            override val presentation = clazz.canonicalName

            override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
                clazz.collectPsiElements(result, visited)
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

    override fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        annotations.forEach { it.collectPsiElements(result, visited) }
        type.collectPsiElements(result, visited)
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
        if (containingLClass.canonicalName != other.containingLClass.canonicalName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + containingLClass.canonicalName.hashCode()
        return result
    }
}