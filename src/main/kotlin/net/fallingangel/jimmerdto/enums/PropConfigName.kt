package net.fallingangel.jimmerdto.enums

enum class PropConfigName(val text: String) {
    Where("!where"),
    OrderBy("!orderBy"),
    Filter("!filter"),
    Recursion("!recursion"),
    FetchType("!fetchType"),
    Limit("!limit"),
    Batch("!batch"),
    Depth("!depth");

    companion object {
        val availableNames = entries.map(PropConfigName::text).joinToString { "'$it'" }
    }
}