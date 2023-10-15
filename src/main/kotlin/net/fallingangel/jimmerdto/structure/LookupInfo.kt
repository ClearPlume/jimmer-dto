package net.fallingangel.jimmerdto.structure

data class LookupInfo(
    val presentation: String,
    val insertion: String,
    val type: String,
    val caretOffset: Int = 0
)
