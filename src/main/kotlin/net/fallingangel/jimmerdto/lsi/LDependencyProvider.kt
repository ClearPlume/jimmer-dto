package net.fallingangel.jimmerdto.lsi

import com.intellij.openapi.util.ModificationTracker

interface LDependencyProvider {
    val dependencyItem: Any
        get() = ModificationTracker.NEVER_CHANGED

    fun collectDependencyItems(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider> = mutableSetOf()) {
        if (!visited.add(this)) {
            return
        }
        result.add(dependencyItem)
        collectChildren(result, visited)
    }

    fun collectChildren(result: MutableSet<Any>, visited: MutableSet<LDependencyProvider>) {}
}
