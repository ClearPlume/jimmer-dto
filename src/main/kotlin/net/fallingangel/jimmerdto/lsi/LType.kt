package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import org.babyfish.jimmer.sql.Embeddable

sealed class LType {
    abstract val name: String
    abstract val nullable: Boolean

    open val canonicalName: String
        get() = name

    val presentableName: String
        get() = buildString {
            when {
                this@LType is EnumType<*, *> -> append("Enum<")
                this@LType is LClass<*> && this@LType.hasAnnotation(Embeddable::class) -> append("Embeddable<")
                else -> append("")
            }

            append(name)

            when {
                this@LType is EnumType<*, *> -> append(">")
                this@LType is LClass<*> && this@LType.hasAnnotation(Embeddable::class) -> append(">")
                else -> append("")
            }
        }

    open val isAnnotation: Boolean
        get() = false

    open fun toDebugString(visited: MutableSet<String>): String = when (this) {
        is ScalarType -> "ScalarType(name=$name, nullable=$nullable)"
        is ArrayType -> "ArrayType(nullable=$nullable, elementType=${elementType.toDebugString(visited)})"
        is CollectionType -> "CollectionType(nullable=$nullable, kind=$kind, elementType=${elementType.toDebugString(visited)})"
        is EnumType<*, *> -> "EnumType(name=$name, canonicalName=$canonicalName, nullable=$nullable, values=$values)"
        is MapType -> "MapType(nullable=$nullable, keyType=${keyType.toDebugString(visited)}, valueType=${valueType.toDebugString(visited)})"
        is LClass<*> -> toDebugString(visited)
    }

    data class ScalarType(
        override val name: String,
        override val nullable: Boolean,
    ) : LType()

    /**
     * @param elementType 元素类型
     */
    data class ArrayType(
        override val nullable: Boolean,
        val elementType: LType,
    ) : LType() {
        override val name: String
            get() = "Array<${elementType.name}>"

        override val canonicalName: String
            get() = "Array<${elementType.canonicalName}>"
    }

    /**
     * @param elementType 元素类型
     */
    data class CollectionType(
        override val nullable: Boolean,
        val elementType: LType,
        val kind: CollectionKind,
    ) : LType() {
        override val name: String
            get() = "$kind<${elementType.name}>"

        override val canonicalName: String
            get() = "$kind<${elementType.canonicalName}>"

        enum class CollectionKind {
            List, Set, Queue
        }
    }

    /**
     * @param name 枚举类型名称
     */
    data class EnumType<E : PsiElement, EP : PsiElement>(
        override val name: String,
        override val canonicalName: String,
        override val nullable: Boolean,
        val values: Map<String, EP>,
        override val source: E,
    ) : LType(), LPsiDependent {
        override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
            if (!visited.add(this)) {
                return
            }
            result.add(source)
        }
    }

    data class MapType(
        override val nullable: Boolean,
        val keyType: LType,
        val valueType: LType,
    ) : LType() {
        override val name: String
            get() = "Map<${keyType.name}, ${valueType.name}>"

        override val canonicalName: String
            get() = "Map<${keyType.canonicalName}>, ${valueType.canonicalName}>"
    }
}