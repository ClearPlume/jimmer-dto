package net.fallingangel.jimmerdto.structure

data class Property(
    val name: String,
    val type: String,
    val annotations: List<String> = emptyList()
)
