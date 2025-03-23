package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.codeInsight.completion.CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
import net.fallingangel.jimmerdto.exception.PropertyNotExistException
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.element.DTONegativeProp
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class PropNegativeProperties : Structure<DTONegativeProp, List<LProperty<*>>> {
    /**
     * @param element DTO或关联属性中的负属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTONegativeProp): List<LProperty<*>> {
        val propPath = element.propPath().dropLastWhile { it == DUMMY_IDENTIFIER_TRIMMED }
        return try {
            element.file.clazz.walk(propPath).properties
        } catch (_: PropertyNotExistException) {
            println("Property not found: $propPath")
            emptyList()
        }
    }
}