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
            if (o.macroName.text == "allScalars") {
                applyStyle(o.firstChild, DTOSyntaxHighlighter.MACRO)
                applyStyle(o.macroName, DTOSyntaxHighlighter.MACRO)
            } else {
                applyStyle(o.firstChild, DTOSyntaxHighlighter.ERROR, HighlightSeverity.ERROR)
                applyStyle(o.macroName, DTOSyntaxHighlighter.ERROR, HighlightSeverity.ERROR)
            }
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
            if (o.firstChild.text in arrayOf("id", "flat")) {
                applyStyle(o.firstChild, DTOSyntaxHighlighter.FUNCTION)
            } else {
                applyStyle(o.firstChild, DTOSyntaxHighlighter.ERROR, HighlightSeverity.ERROR)
            }
        }

        private fun applyStyle(element: PsiElement, style: TextAttributesKey, severity: HighlightSeverity = HighlightSeverity.INFORMATION) {
            holder.newSilentAnnotation(severity)
                    .range(element)
                    .textAttributes(style)
                    .create()
        }
    }
}
