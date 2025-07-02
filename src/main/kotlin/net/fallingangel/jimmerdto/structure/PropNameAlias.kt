package net.fallingangel.jimmerdto.structure

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

/**
 * TODO 改造结构，描述属性使用情况(冲突处)，属性别名情况(定义处)，负属性可用名称
 */
data class PropNameAlias(
    val name: String,
    val alias: String? = null,
)

operator fun Map<PropNameAlias, Int>.get(name: String): Int? {
    return entries.firstOrNull { (key) -> key.alias == name }?.value
        ?: entries.firstOrNull { (key) -> key.name == name }?.value
}

/**
 * 属性重复使用：需要记录属性使用次数
 *
 * 用户属性重名：需要记录已经使用过的属性名称和别名
 *
 * 负属性是否可以正确移除属性：需要记录已经使用过的属性名称和别名
 */
data class PropUsage(
    /**
     * 属性名称 to 属性元素
     */
    val props: Map<String, DTOElement>,

    /**
     * 别名 to 别名来源
     */
    val aliases: Map<String, DTOElement>,

    /**
     * 重复定义的名称列表，包含原始名称和别名
     */
    val duplicated: List<String>,
)