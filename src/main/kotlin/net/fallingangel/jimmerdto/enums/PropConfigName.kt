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
        val availableNames = values().map(PropConfigName::text).joinToString { "'$it'" }
    }
}