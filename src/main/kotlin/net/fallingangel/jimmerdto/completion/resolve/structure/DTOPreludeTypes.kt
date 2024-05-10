package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType

class DTOPreludeTypes : Structure<DTOFile, List<String>> {
    /**
     * @param element DTO文件
     *
     * @return DTO文件中，内置的类型列表
     */
    override fun value(element: DTOFile): List<String> {
        val basicTypes = BasicType.types()
        val genericTypes = GenericType.types().map { it.presentation }

        return basicTypes + genericTypes
    }
}