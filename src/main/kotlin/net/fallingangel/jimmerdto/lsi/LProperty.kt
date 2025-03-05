package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.*
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

    fun doesTypeHaveAnnotation(annotationClass: KClass<out Annotation>): Boolean {
        return when (type) {
            is LClass<*> -> type.hasAnnotation(annotationClass)
            is LType.CollectionType -> (type.elementType as? LClass<*>)?.hasAnnotation(annotationClass) ?: false
            else -> false
        }
    }

    fun doesTypeHaveExactlyOneAnnotation(vararg annotationClasses: KClass<out Annotation>): Boolean {
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
        }
    }

    fun toDebugString(visited: MutableSet<String>): String {
        val annotationsStr = annotationsToString(visited)
        return "LProperty(name=$name, type=${type.toDebugString(visited)}, annotations=$annotationsStr)"
    }
}