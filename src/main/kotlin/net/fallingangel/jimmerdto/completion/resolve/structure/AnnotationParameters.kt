package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTOAnnotation
import net.fallingangel.jimmerdto.psi.DTOImport

class AnnotationParameters : Structure<DTOAnnotation, List<String>> {
    /**
     * @param element 注解元素
     *
     * @return 注解参数列表
     */
    override fun value(element: DTOAnnotation): List<String> {
        val project = element.project
        val name = element.annotationConstructor.qualifiedName?.qualifiedNamePartList ?: throw IllegalStateException()

        val clazz = if (name.size == 1) {
            val imports = PsiTreeUtil.findChildrenOfType(element.containingFile, DTOImport::class.java)
            val import = imports.find { it.qualifiedType.qualifiedTypeName.qualifiedName.qualifiedNamePartList.last().text == name.first().text }
            import ?: throw IllegalStateException()
            import.qualifiedType.text
        } else {
            name.joinToString(".")
        }
        val annotation = JavaPsiFacade.getInstance(project).findClass(clazz, ProjectScope.getAllScope(project)) ?: throw IllegalStateException()
        return annotation.allMethods.map { it.name }
    }
}
