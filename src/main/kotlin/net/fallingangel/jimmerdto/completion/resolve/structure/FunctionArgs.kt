package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.exception.PropertyNotExistException
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOPropArg
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class FunctionArgs : Structure<DTOPropArg, List<LProperty<*>>> {
    /**
     * @param element 方法参数元素
     *
     * @return 可用方法参数
     */
    override fun value(element: DTOPropArg): List<LProperty<*>> {
        val prop = element.parent as DTOPositiveProp
        val propPath = prop.propPath().dropLast(1)
        val propName = prop.name.value
        val properties = try {
            element.file.clazz.walk(propPath).properties
        } catch (_: PropertyNotExistException) {
            println("Property not found: $propPath")
            emptyList()
        }

        return when (propName) {
            in Function.values().map { it.expression } -> {
                properties
                        .filter { property ->
                            val function = Function.values().first { it.expression == propName }
                            function.argType.test(property)
                        }
            }

            in SpecFunction.values().map { it.expression } -> {
                properties
                        .filter { property ->
                            val specFunction = SpecFunction.values().first { it.expression == propName }
                            specFunction.argType.test(property)
                        }
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }
}