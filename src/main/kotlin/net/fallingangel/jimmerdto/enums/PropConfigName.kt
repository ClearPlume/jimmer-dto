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
        val availableNames = entries.map(PropConfigName::text)

        val exclusive = listOf(Where to Filter, OrderBy to Filter, Recursion to Depth)
            .flatMap { (a, b) -> listOf(a.text to b.text, b.text to a.text) }
            .groupBy({ it.first }, { it.second })
    }
}