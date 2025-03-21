package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.annotation.annotationsToString

/**
 * @param C 类级别Psi元素类型
 * @param source 类对应Psi元素
 */
data class LClass<C : PsiElement>(
    override val name: String,
    override val canonicalName: String,
    override val nullable: Boolean,
    override val isAnnotation: Boolean,
    override val annotations: List<LAnnotation<*>>,
    val parentsHolder: Lazy<List<LClass<C>>>,
    val propertiesHolder: Lazy<List<LProperty<*>>>,
    val methodsHolder: Lazy<List<LMethod<*>>>,
    override val source: C,
) : LType(), LElement, LAnnotationOwner, LPsiDependent {
    val parents by parentsHolder

    val allParents: List<LClass<out PsiElement>>
        get() = parents + parents.flatMap(LClass<*>::parents)

    val properties by propertiesHolder

    val allProperties: List<LProperty<*>>
        get() = properties + allParents.flatMap(LClass<*>::properties)

    val methods by methodsHolder

    /**
     * 以this为起点，走过[propPath]后，属性的LClass
     */
    fun walk(propPath: List<String>) = if (propPath.isEmpty()) {
        this
    } else {
        property(propPath).actualType as LClass<*>
    }

    /**
     * 以this为起点，走过[propPath]后的属性
     *
     * @param propPath 不可为空
     */
    fun property(propPath: List<String>) = findProperty(propPath)

    /**
     * 以this为起点，走过[propPath]后的属性
     */
    fun propertyOrNull(propPath: List<String>): LProperty<*>? {
        if (propPath.isEmpty()) {
            return null
        }
        return try {
            findProperty(propPath)
        } catch (e: Exception) {
            null
        }
    }

    override fun toString() = toDebugString(mutableSetOf())

    override fun toDebugString(visited: MutableSet<String>): String {
        val id = this.canonicalName
        if (id in visited) return "$id↺"  // 发现递归引用，返回带↺的标识

        visited += id // 标记当前节点已访问

        val annotationStr = annotationsToString(visited)
        val parentsStr = parents.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
        val propertiesStr = properties.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
        val methodsStr = methods.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }

        visited -= id // 离开当前节点时，解除标记（允许其它路径访问）

        return buildString {
            append("LClass(")
            append("name=$name, ")
            append("canonicalName=$canonicalName, ")
            append("nullable=$nullable, ")
            append("isAnnotation=$isAnnotation, ")
            append("annotations=$annotationStr, ")
            append("parents=$parentsStr, ")
            append("properties=$propertiesStr, ")
            append("methods=$methodsStr, ")
            append("source=$source, ")
            append(")")
        }
    }

    override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        if (!visited.add(this)) {
            return
        }
        result.add(source)
        annotations.forEach { it.collectPsiElements(result, visited) }
        parents.forEach { it.collectPsiElements(result, visited) }
        properties.forEach { it.collectPsiElements(result, visited) }
        methods.forEach { it.collectPsiElements(result, visited) }
    }
}