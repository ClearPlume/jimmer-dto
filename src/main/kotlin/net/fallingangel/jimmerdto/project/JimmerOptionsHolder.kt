package net.fallingangel.jimmerdto.project

import com.intellij.openapi.components.*

@State(name = "JimmerOptions", storages = [Storage(StoragePathMacros.MODULE_FILE)])
class JimmerOptionsHolder : SimplePersistentStateComponent<JimmerOptionsHolder.State>(State()) {
    class State : BaseState() {
        var options by map<String, String>()
    }

    @Suppress("UnstableApiUsage")
    var raw: Map<String, String>
        get() = state.options
        set(value) {
            state.options.clear()
            state.options.putAll(value)
            state.intIncrementModificationCount()
        }
}
