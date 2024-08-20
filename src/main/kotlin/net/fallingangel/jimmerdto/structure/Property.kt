package net.fallingangel.jimmerdto.structure

import net.fallingangel.jimmerdto.enums.RelationType

data class Property(
    val name: String,
    val type: String,
    val nullable: Boolean = false,
    val annotations: List<String> = emptyList()
) {
    val simpleAnnotations: List<String>
        get() = annotations.map { it.substringAfterLast('.') }

    val whetherAssociated: Boolean
        get() = simpleAnnotations.any { it in RelationType.all }
}
