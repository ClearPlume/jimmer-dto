package net.fallingangel.jimmerdto

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.get
import net.fallingangel.jimmerdto.util.propPath

class DTODocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val macro = element.parent as? DTOMacro ?: return null
        val macroClass = macro.file.clazz.walk(macro.propPath())
        val argList = macro.args?.values?.map(PsiElement::getText) ?: macro[StructureType.MacroTypes]
        val containThisProp = argList.any { it in listOf("this", macroClass.name) }

        val thisPropList = if (containThisProp) {
            macroClass.properties.map { macroClass to it }
        } else {
            emptyList()
        }
        val superPropList = macroClass.allParents
                .filter { argList.isEmpty() || argList.contains(it.name) }
                .flatMap { clazz -> clazz.properties.map { clazz to it } }

        return (thisPropList + superPropList)
                .groupBy { it.first }
                .mapValues { entry -> entry.value.map { it.second } }
                .map { (clazz, props) ->
                    val propsString = props.joinToString("\n") {
                        """
                            $SECTION_HEADER_START
                            ${it.presentableType}
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