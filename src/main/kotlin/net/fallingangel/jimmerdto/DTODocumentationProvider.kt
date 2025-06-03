package net.fallingangel.jimmerdto

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOMacro

class DTODocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val macro = element.parent as? DTOMacro ?: return null
        if (macro.name.value !in listOf("allScalars", "allReferences")) {
            return null
        }
        val isScalar = macro.name.value == "allScalars"
        val macroClass = macro.clazz
        val argList = macro.args?.values?.map(PsiElement::getText) ?: macro.types
        val containThisProp = argList.any { it in listOf("this", macroClass.name) }

        val thisPropList = if (containThisProp) {
            macroClass.properties
                    .filter { if (isScalar) !it.isEntityAssociation else it.isReference }
                    .map { macroClass to it }
        } else {
            emptyList()
        }
        val superPropList = macroClass.allParents
                .filter { argList.isEmpty() || argList.contains(it.name) }
                .flatMap { clazz ->
                    clazz.properties
                            .filter { if (isScalar) !it.isEntityAssociation else it.isReference }
                            .map { clazz to it }
                }

        return (thisPropList + superPropList)
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
    }
}