package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiMethod
import net.fallingangel.jimmerdto.psi.DTONestAnnotation
import net.fallingangel.jimmerdto.util.psiClass

class NestAnnotationParameters : Structure<DTONestAnnotation, List<PsiMethod>> {
    /**
     * @param element 注解元素
     *
     * @return 注解参数列表
     */
    override fun value(element: DTONestAnnotation): List<PsiMethod> {
        return element.annotationName.psiClass().methods.toList()
    }
}
