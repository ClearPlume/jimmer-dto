package net.fallingangel.jimmerdto.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
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
            val macroAvailableParams = o[StructureType.MacroTypes]
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
                val propAvailableArgs = propArgs[StructureType.FunctionArgs].map { it.name }
                if (propArgs.value.text !in propAvailableArgs) {
                    propArgs.value.error()
                }
            }
            // 当前属性为非方法属性
            if (o.propArgs == null) {
                val properties = o.propName[StructureType.PropProperties]
                properties.find { it.name == o.propName.text } ?: o.propName.error()
            }
        }

        override fun visitNegativeProp(o: DTONegativeProp) {
            val properties = o[StructureType.PropNegativeProperties]
            if (properties.find { it.name == o.identifier?.text } != null) {
                o.style(DTOSyntaxHighlighter.NOT_USED)
            } else {
                o.firstChild.style(DTOSyntaxHighlighter.NOT_USED)
                o.identifier?.error()
            }
        }

        /**
         * 为注解参数名上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            o.firstChild.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.firstChild.next(DTOTypes.EQUALS).style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
        }

        /**
         * 为Dto名称或其父级实体上色
         */
        override fun visitDtoName(o: DTODtoName) {
            val parent = o.parent
            if (parent is DTODtoSupers) {
                val availableSupers = parent[StructureType.DtoSupers]

                for (superDto in parent.dtoNameList) {
                    if (parent.dtoNameList.count { it.name == o.name } != 1) {
                        o.error(DTOSyntaxHighlighter.DUPLICATION)
                    }
                    if (o.name !in availableSupers) {
                        o.error()
                    }
                }
            }
        }

        /**
         * 为枚举映射上色
         */
        override fun visitEnumInstanceMapping(o: DTOEnumInstanceMapping) {
            val enumBody = o.parent as DTOEnumBody
            val availableEnums = o.enumInstance[StructureType.EnumValues]
            val currentEnumNames = enumBody.enumInstanceMappingList.map { it.enumInstance.text }
            val currentEnumValues = enumBody.enumInstanceMappingList.mapNotNull { it.enumInstanceValue }

            val allInt = currentEnumValues.all { it.text.matches(Regex("\\d+")) }
            val allString = currentEnumValues.all { it.text.matches(Regex("\".+\"")) }
            val valueTypeValid = allInt || allString

            if (o.enumInstance.text in availableEnums) {
                o.enumInstance.style(DTOSyntaxHighlighter.ENUM_INSTANCE)
            } else {
                o.enumInstance.error()
            }
            if (currentEnumNames.count { it == o.enumInstance.text } != 1) {
                o.enumInstance.error(DTOSyntaxHighlighter.DUPLICATION)
            }
            if (!valueTypeValid) {
                o.enumInstanceValue?.error()
            }
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
