package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.codeInsight.completion.CompletionUtilCore
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class PropProperties : Structure<DTOPositiveProp, List<Property>> {
    /**
     * @param element DTO或关联属性中的属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTOPositiveProp): List<Property> {
        val propPath = if (element.text == CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED) {
            element.propPath()
        } else {
            element.upper.propPath()
        }
        return if (element.haveUpper) {
            element.virtualFile.properties(
                element.project,
                if (propPath.last() == CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED) {
                    propPath - CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
                } else {
                    propPath
                }
            )
        } else {
            element.virtualFile.properties(element.project)
        }
    }
}