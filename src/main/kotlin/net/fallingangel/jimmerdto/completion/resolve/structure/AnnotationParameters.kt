package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiMethod
import net.fallingangel.jimmerdto.psi.DTOAnnotation
import net.fallingangel.jimmerdto.util.psiClass

class AnnotationParameters : Structure<DTOAnnotation, List<PsiMethod>> {
    /**
     * @param element 注解元素
     *
     * @return 注解参数列表
     */
    override fun value(element: DTOAnnotation): List<PsiMethod> {
        val annotation = element.annotationConstructor.annotationName!!.psiClass()
        return annotation.methods.toList()
    }
}