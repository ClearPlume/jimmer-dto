package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.*
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.Transient
import kotlin.reflect.KClass

data class LProperty<P : PsiElement>(
    override val name: String,
    override val annotations: List<LAnnotation<*>>,
    override val type: LType,
    override val source: P,
) : LElement, LAnnotationOwner, LNullableAware, LPsiDependent {
    val actualType = if (type is LType.CollectionType) {
        type.elementType
    } else {
        type
    }

    val presentableType = buildString {
        append(type.presentableName)
        if (nullable) {
            append("?")
        }
    }

    val isEntityAssociation = doesTypeHaveAnnotation(Entity::class)

    val isAssociation = doesTypeHaveAnnotation(Immutable::class) ||
            doesTypeHaveExactlyOneAnnotation(
                Entity::class,
                MappedSuperclass::class,
                Embeddable::class,
            )

    val isList = type is LType.CollectionType

    val isTransient = hasAnnotation(Transient::class)

    val isReference = isEntityAssociation && !isList && !isTransient

    fun doesTypeHaveAnnotation(annotationClass: KClass<out Annotation>): Boolean {
        return when (type) {
            is LClass<*> -> type.hasAnnotation(annotationClass)
            is LType.CollectionType -> (type.elementType as? LClass<*>)?.hasAnnotation(annotationClass) ?: false
            else -> false
        }
    }

    private fun doesTypeHaveExactlyOneAnnotation(vararg annotationClasses: KClass<out Annotation>): Boolean {
        return when (type) {
            is LClass<*> -> type.hasExactlyOneAnnotation(*annotationClasses)
            is LType.CollectionType -> (type.elementType as? LClass<*>)?.hasExactlyOneAnnotation(*annotationClasses) ?: false
            else -> false
        }
    }

    override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        if (!visited.add(this)) {
            return
        }
        result.add(source)
        annotations.forEach { it.collectPsiElements(result, visited) }
        if (type is LClass<*>) {
            type.collectPsiElements(result, visited)
        } else if (type is LType.EnumType<*, *>) {
            type.collectPsiElements(result, visited)
        }
    }

    fun toDebugString(visited: MutableSet<String>): String {
        val annotationsStr = annotationsToString(visited)
        return "LProperty(name=$name, type=${type.toDebugString(visited)}, annotations=$annotationsStr)"
    }
}