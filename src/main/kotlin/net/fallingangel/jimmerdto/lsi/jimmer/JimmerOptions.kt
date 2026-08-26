package net.fallingangel.jimmerdto.lsi.jimmer

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.project.JimmerOptionsHolder

class JimmerOptions private constructor(raw: Map<String, String>) {
    val keepIsPrefix = raw["jimmer.keepIsPrefix"].toBoolean()

    val defaultNullableInputModifier = raw["jimmer.dto.defaultNullableInputModifier"]
        ?.let { text -> Modifier.entries.filter { it.isInputStrategy }.find { it.name.equals(text, true) } }
        ?: Modifier.Static

    companion object {
        fun of(module: Module): JimmerOptions {
            return JimmerOptions(module.service<JimmerOptionsHolder>().raw)
        }
    }
}
