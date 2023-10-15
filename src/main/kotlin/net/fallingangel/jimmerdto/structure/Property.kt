package net.fallingangel.jimmerdto.structure

data class Property(
    val name: String,
    val type: String,
    val nullable: Boolean = false,
    val annotations: List<String> = emptyList()
)
