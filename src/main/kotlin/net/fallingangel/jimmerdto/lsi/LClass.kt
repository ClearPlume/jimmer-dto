package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.annotation.annotationsToString

/**
 * 三个 [Lazy] 持有者和 children 的 lambda 在 [LanguageProcessor.lClass] 中构造，
 * 可能捕获尚未完成赋值的 lateinit 自引用。
 * 禁止在本类的构造过程（init 块、非 lazy 属性初始化器）中触发任何 Lazy 的求值。
 */
class LClass(
    val lName: LName,
    annotationsHolder: Lazy<List<LAnnotation>>,
    parentsHolder: Lazy<List<LClass>>,
    val childrenProvider: () -> List<LClass>,
    propertiesHolder: Lazy<List<LProperty>>,
    override val dependencyItem: PsiNamedElement,
) : LElement, LAnnotationOwner, LDependencyProvider {
    override val name = lName.name

    val fqName = lName.fqName

    override val annotations by annotationsHolder

    val parents by parentsHolder

    val allParents by lazy {
        buildList {
            val visited = mutableSetOf(fqName)
            fun collect(cls: LClass) {
                for (parent in cls.parents) {
                    if (visited.add(parent.fqName)) {
                        add(parent)
                        collect(parent)
                    }
                }
            }
            collect(this@LClass)
        }
    }

    val children: List<LClass>
        get() = childrenProvider()

    val properties by propertiesHolder

    val allProperties by lazy {
        properties + allParents.flatMap(LClass::properties)
    }

    fun findProperty(name: String?): LProperty? {
        return allProperties.find { it.name == name }
    }

    override fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {
        annotations.forEach { it.collectDependencyItems(result, visited) }
        parents.forEach { it.collectDependencyItems(result, visited) }
        properties.forEach { it.collectDependencyItems(result, visited) }
    }

    override fun toString() = toDebugString(mutableSetOf())

    fun toDebugString(visited: MutableSet<String>): String {
        val id = fqName
        if (id in visited) return "$id↺"  // 发现递归引用，返回带↺的标识

        visited += id // 标记当前节点已访问

        val annotationStr = annotationsToString(visited)
        val parentsStr = parents.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
        val propertiesStr = properties.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }

        visited -= id // 离开当前节点时，解除标记（允许其它路径访问）

        return buildString {
            append("LClass(")
            append("name=$name, ")
            append("fqName=$fqName, ")
            append("annotations=$annotationStr, ")
            append("parents=$parentsStr, ")
            append("properties=$propertiesStr, ")
            append("dependencyItem=$dependencyItem, ")
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LClass

        return lName == other.lName
    }

    override fun hashCode(): Int {
        return lName.hashCode()
    }
}
