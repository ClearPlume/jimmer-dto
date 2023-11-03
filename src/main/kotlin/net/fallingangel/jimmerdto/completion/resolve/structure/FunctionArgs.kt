package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.PredicateFunction
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropArgs
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class FunctionArgs : Structure<DTOPropArgs, List<Property>> {
    /**
     * @param element 方法参数元素
     *
     * @return 可用方法参数
     */
    override fun value(element: DTOPropArgs): List<Property> {
        val prop = element.parent as DTOPositiveProp
        val propPath = if (prop.haveUpper) {
            prop.upper.propPath()
        } else {
            emptyList()
        }
        val propName = prop.propName.text
        val properties = element.virtualFile.properties(element.project, propPath)

        return when (propName) {
            in Function.values().map { it.expression } -> {
                properties
                        .filter { property ->
                            val function = Function.values().first { it.expression == propName }
                            function.argType.test(property)
                        }
            }

            in PredicateFunction.values().map { it.expression } -> {
                properties
                        .filter { property ->
                            val predicateFunction = PredicateFunction.values().first { it.expression == propName }
                            predicateFunction.argType.test(property)
                        }
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }
}