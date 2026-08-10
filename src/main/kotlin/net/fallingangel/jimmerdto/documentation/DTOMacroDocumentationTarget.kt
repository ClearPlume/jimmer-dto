package net.fallingangel.jimmerdto.documentation

import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.createSmartPointer
import net.fallingangel.jimmerdto.psi.element.DTOMacro

@Suppress("UnstableApiUsage")
class DTOMacroDocumentationTarget(private val macro: DTOMacro) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> {
        val pointer = macro.createSmartPointer()
        return Pointer { pointer.element?.let(::DTOMacroDocumentationTarget) }
    }

    override fun computePresentation(): TargetPresentation {
        return TargetPresentation.builder(macro.name.value).presentation()
    }

    override fun computeDocumentation(): DocumentationResult? {
        // TODO 优化可选项获取逻辑
        if (macro.name.value !in listOf("allScalars", "allReferences")) {
            return null
        }

        val html = macro.carriedProps
            .map { it.containingLClass to it }
            .groupBy { it.first }
            .mapValues { entry -> entry.value.map { it.second } }
            .map { (clazz, props) ->
                val propsString = props.joinToString("\n") {
                    val type = it.presentableType
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    """
                        $SECTION_HEADER_START
                        $type
                        $SECTION_SEPARATOR
                        <p>
                        ${it.name}
                        $SECTION_END
                        </tr>
                    """.trimIndent()
                }

                """
                    $DEFINITION_START${clazz.canonicalName}$DEFINITION_END
                    $SECTIONS_START
                    $propsString
                    $SECTIONS_END
                """.trimIndent()
            }.joinToString("\n")
        return DocumentationResult.documentation(html)
    }
}