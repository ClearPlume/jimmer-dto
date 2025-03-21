package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.util.modifiedBy

class DtoModifiers : Structure<DTODto, List<String>> {
    /**
     * @param element Dto名称元素
     *
     * @return Dto修饰符列表
     */
    override fun value(element: DTODto): List<String> {
        val allModifiers = Modifier.values().toMutableList()
        val dtoModifiers = element.modifiers

        if (element modifiedBy Modifier.Input) {
            allModifiers -= Modifier.Specification
        }
        if (element modifiedBy Modifier.Specification) {
            allModifiers -= Modifier.Input
        }
        return allModifiers
                .filter { it !in dtoModifiers }
                .map { it.name.lowercase() }
    }
}