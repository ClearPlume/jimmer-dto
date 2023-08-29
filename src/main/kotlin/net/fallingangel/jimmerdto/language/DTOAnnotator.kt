package net.fallingangel.jimmerdto.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.language.psi.*

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

        private fun applyStyle(element: PsiElement, style: TextAttributesKey) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(style)
                    .create()
        }
    }
}
