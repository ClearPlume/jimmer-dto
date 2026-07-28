package net.fallingangel.jimmerdto.lsi

@JvmInline
value class ResolvedTypes private constructor(private val cache: MutableMap<String, LClass>) {
    constructor() : this(mutableMapOf())

    constructor(known: Pair<String, LClass>) : this(mutableMapOf(known))

    fun getOrPut(key: String, defaultValue: () -> LClass): LClass {
        return cache.getOrPut(key, defaultValue)
    }
}