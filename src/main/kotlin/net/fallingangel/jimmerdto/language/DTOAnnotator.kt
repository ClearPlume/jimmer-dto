package net.fallingangel.jimmerdto.language

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.language.psi.DTOTypes

/**
 * 部分代码结构的高亮
 */
class DTOAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.elementType == DTOTypes.ANNOTATION_CONSTRUCTOR) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(DTOSyntaxHighlighter.ANNOTATION)
                    .create()
        }
    }
}
