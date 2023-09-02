package net.fallingangel.jimmerdto.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.*

/**
 * 部分代码结构的高亮
 */
class DTOAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(DTOAnnotatorVisitor(holder))
    }

    private class DTOAnnotatorVisitor(private val holder: AnnotationHolder) : DTOVisitor() {
        override fun visitAnnotationConstructor(o: DTOAnnotationConstructor) {
            applyStyle(o, DTOSyntaxHighlighter.ANNOTATION)
        }

        override fun visitNestAnnotation(o: DTONestAnnotation) {
            applyStyle(o.qualifiedName, DTOSyntaxHighlighter.ANNOTATION)
        }

        override fun visitMacro(o: DTOMacro) {
            applyStyle(o.firstChild, DTOSyntaxHighlighter.MACRO)
            applyStyle(o.macroName, DTOSyntaxHighlighter.MACRO)
        }

        /**
         * 为as组的『as』上色
         */
        override fun visitAliasGroup(o: DTOAliasGroup) {
            applyStyle(o.firstChild, DTOSyntaxHighlighter.FUNCTION)
        }

        /**
         * 为方法上色
         */
        override fun visitFunctionProp(o: DTOFunctionProp) {
            applyStyle(o.firstChild, DTOSyntaxHighlighter.FUNCTION)
        }

        private fun applyStyle(element: PsiElement, style: TextAttributesKey) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(style)
                    .create()
        }
    }
}
