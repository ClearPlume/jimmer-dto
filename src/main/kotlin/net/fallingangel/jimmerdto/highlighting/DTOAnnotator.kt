package net.fallingangel.jimmerdto.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.util.get

/**
 * 部分代码结构的高亮
 */
class DTOAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(DTOAnnotatorVisitor(holder))
    }

    private class DTOAnnotatorVisitor(private val holder: AnnotationHolder) : DTOVisitor() {
        /**
         * 为注解上色
         */
        override fun visitAnnotationConstructor(o: DTOAnnotationConstructor) {
            o.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为作为参数的注解上色
         */
        override fun visitNestAnnotation(o: DTONestAnnotation) {
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            val macroName = o.macroName!!
            if (macroName.text == "allScalars") {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                o.firstChild.error()
                macroName.error()
            }
        }

        /**
         * 为宏参数上色
         */
        override fun visitMacroArgs(o: DTOMacroArgs) {
            val macroAvailableParams = if (o.parent.parent.parent.parent is DTODto) {
                o[StructureType.MacroTypes]
            } else {
                o[StructureType.RelationMacroTypes]
            }

            for (macroArg in o.qualifiedNameList) {
                if (macroArg.text !in macroAvailableParams) {
                    macroArg.error()
                }
                if (o.qualifiedNameList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(DTOSyntaxHighlighter.DUPLICATION)
                }
            }
        }

        /**
         * 为as组上色
         *
         * as(original -> replacement) { ... }
         */
        override fun visitAliasGroup(o: DTOAliasGroup) {
            // 『as』
            o.firstChild.style(DTOSyntaxHighlighter.FUNCTION)

            // original
            val original = o.aliasPattern.original
            if (original.firstChild.text == "^" && original.lastChild.text.last() == '$') {
                original.error()
            }
        }

        /**
         * 为属性上色
         */
        override fun visitPositiveProp(o: DTOPositiveProp) {
            // 当前属性为方法
            if (o.propArgs != null) {
                // 方法名
                val propArgs = o.propArgs!!
                if (o.propName.text in arrayOf("id", "flat")) {
                    o.propName.style(DTOSyntaxHighlighter.FUNCTION)
                } else {
                    o.propName.error()
                }

                // 方法参数
                val propAvailableArgs = if (propArgs.parent.parent.parent.parent is DTODto) {
                    propArgs[StructureType.PropArgs].map { it.name }
                } else {
                    propArgs[StructureType.RelationPropArgs].map { it.name }
                }
                if (propArgs.value.text !in propAvailableArgs) {
                    propArgs.value.error()
                }
            }
            // 当前属性为非方法属性
            if (o.propArgs == null) {
                val properties = if (o.parent.parent.parent is DTODto) {
                    o.propName[StructureType.DtoProperties]
                } else {
                    o.propName[StructureType.RelationProperties]
                }
                properties.find { it.name == o.propName.text } ?: o.propName.error()
            }
        }

        override fun visitNegativeProp(o: DTONegativeProp) {
            o.style(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
        }

        /**
         * 为参数名上色
         */
        override fun visitNamedParameter(o: DTONamedParameter) {
            o.parameterName.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.parameterName.next(DTOTypes.EQUALS).style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
        }

        /**
         * 为参数名上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            o.firstChild.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.firstChild.next(DTOTypes.EQUALS).style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
        }

        private fun PsiElement.next(elementType: IElementType): PsiElement {
            return nextLeaf { it.elementType == elementType }!!
        }

        private fun PsiElement.style(style: TextAttributesKey) = annotator(style, HighlightSeverity.INFORMATION)

        private fun PsiElement.error(style: TextAttributesKey = DTOSyntaxHighlighter.ERROR) = annotator(style, HighlightSeverity.ERROR)

        private fun PsiElement.annotator(style: TextAttributesKey, severity: HighlightSeverity) {
            holder.newSilentAnnotation(severity)
                    .range(this)
                    .textAttributes(style)
                    .create()
        }
    }
}
