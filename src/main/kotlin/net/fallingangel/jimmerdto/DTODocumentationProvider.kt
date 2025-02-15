package net.fallingangel.jimmerdto

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.DTOMacro
import net.fallingangel.jimmerdto.psi.DTOMacroArg
import net.fallingangel.jimmerdto.util.DTOPsiUtil
import net.fallingangel.jimmerdto.util.clazz
import net.fallingangel.jimmerdto.util.get
import net.fallingangel.jimmerdto.util.supers
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext

class DTODocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val macro = element.parent
        if (macro !is DTOMacro) {
            return null
        }
        val project = macro.project
        val macroClass = DTOPsiUtil.resolveMacroThis(macro) ?: return null
        val argList = macro.macroArgs?.macroArgList?.map(DTOMacroArg::getText) ?: macro[StructureType.MacroTypes]
        val containThisProp = argList.any { it in listOf("this", macroClass.name) }

        return when (macroClass.language) {
            JavaLanguage.INSTANCE -> {
                val thisClass = macroClass as PsiClass
                val thisPropList = if (containThisProp) {
                    thisClass.methods.toList()
                } else {
                    emptyList()
                }

                val superPropList = thisClass.supers()
                        .filter { argList.isEmpty() || argList.contains(it.name) }
                        .flatMap { it.methods.toList() }
                (thisPropList + superPropList)
                        .groupBy { it.containingClass!! }
                        .renderArg()
            }

            KotlinLanguage.INSTANCE -> {
                val ktClass = PsiTreeUtil.findChildOfType(macroClass.containingFile.virtualFile.toPsiFile(project), KtClass::class.java)
                val thisClass = ktClass as KtClass
                val thisPropList = if (containThisProp) {
                    thisClass.getProperties()
                } else {
                    emptyList()
                }

                val superPropList = thisClass.supers()
                        .filter { argList.isEmpty() || argList.contains(it.name) }
                        .flatMap(KtClass::getProperties)
                (thisPropList + superPropList)
                        .groupBy { it.containingClass()!! }
                        .renderArg()
            }

            else -> throw IllegalStateException("Unsupported language: ${macroClass.language}")
        }
    }

    @JvmName("renderArgJ")
    private fun Map<PsiClass, List<PsiMethod>>.renderArg(): String {
        return map { (clazz, props) ->
            val propsString = props.joinToString("\n") {
                val nullable = it.annotations.any { annotation -> annotation.qualifiedName!!.substringAfterLast('.') in listOf("Null", "Nullable") }
                val propType = it.returnType
                """
                    $SECTION_HEADER_START
                    ${propType?.clazz()?.name ?: propType?.presentableText}${if (nullable) "?" else ""}
                    $SECTION_SEPARATOR
                    <p>
                    ${it.name}
                    $SECTION_END
                    </tr>
                """.trimIndent()
            }

            """
                $DEFINITION_START${clazz.qualifiedName}$DEFINITION_END
                $SECTIONS_START
                $propsString
                $SECTIONS_END
            """.trimIndent()
        }.joinToString("\n")
    }

    @JvmName("renderArgK")
    private fun Map<KtClass, List<KtProperty>>.renderArg(): String {
        return map { (clazz, properties) ->
            val propertiesString = properties.joinToString(separator = "\n") {
                val propType = it.analyze()[BindingContext.TYPE, it.typeReference]
                """
                    $SECTION_HEADER_START
                    ${propType?.clazz()?.name ?: propType}
                    $SECTION_SEPARATOR
                    <p>
                    ${it.name}
                    $SECTION_END
                    </tr>
                """.trimIndent()
            }

            """
                $DEFINITION_START${clazz.fqName?.asString()}$DEFINITION_END
                $SECTIONS_START
                $propertiesString
                $SECTIONS_END
            """.trimIndent()
        }.joinToString("\n")
    }
}