package net.fallingangel.jimmerdto.lsi.jimmer

import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.module.Module

class JimmerOptions private constructor(raw: Map<String, String>) {
    val keepIsPrefix = raw["jimmer.keepIsPrefix"].toBoolean()

    companion object {
        fun of(module: Module?): JimmerOptions {
            module ?: return JimmerOptions(emptyMap())
            return JimmerOptions(
                CompilerConfiguration.getInstance(module.project)
                    .getAnnotationProcessingConfiguration(module)
                    .processorOptions
            )
        }
    }
}