@file:Suppress("LanguageDetectionInspection")

package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.annotation.annotationsToString
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.*

data class LProperty<P : PsiElement>(
    override val name: String,
    override val annotations: List<LAnnotation<*>>,
    override val type: LType,
    override val source: P,
    val containingLClass: LClass<*>,
) : LElement, LAnnotationOwner, LNullableAware, LPsiDependent {
    val actualType = if (type is LType.CollectionType) {
        type.elementType
    } else {
        type
    }

    val targetClass: LClass<*>?
        get() = actualType as? LClass<*>

    val presentableType = buildString {
        append(type.presentableName)
        if (nullable) {
            append("?")
        }
    }

    /**
     * 目标类型标注了 [Embeddable]——值对象，字段内嵌在宿主表里，没有外键和独立 id。
     * 结构上像关联（有嵌套属性），语义上不是。
     */
    val isEmbedded = targetClass?.hasAnnotation(Embeddable::class) == true

    /**
     * 目标类型标注了 [Entity]——真正的关联：有外键，有独立表，有 id。
     * 等价于编译器的 isAssociation(true)。
     *
     * 编译器另有 isAssociation(false)，即 isImmutable：目标类型标注了
     * [Entity]/[MappedSuperclass]/[Embeddable] 三者之一（互斥），
     * 或者单独标注了 [Immutable]（[Immutable] 可与任一 SQL 注解共存，
     * 但共存时以 SQL 注解为准）。
     * 在插件语境里 [MappedSuperclass] 不出现在属性类型位置，
     * [Immutable] 不出现在 SQL 实体中，实际命中的只有 [Entity] 和 [Embeddable]，
     * 所以不需要这个并集——每个场景直接用 isEntityAssociation 或 isEmbedded。
     */
    val isEntityAssociation = targetClass?.hasAnnotation(Entity::class) == true

    val isFormula = hasAnnotation(Formula::class)

    val isId = hasAnnotation(Id::class)

    val isKey = hasAnnotation(Key::class)

    val isRecursive = isEntityAssociation && containingLClass == targetClass

    val isList = type is LType.CollectionType

    val isTransient = hasAnnotation(Transient::class)

    /**
     * 单引用实体关联——实体关联、非集合、非 transient。
     * 等价于编译器的 isAutoReference 判断。
     */
    val isReference = isEntityAssociation && !isList && !isTransient

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LProperty<*>

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