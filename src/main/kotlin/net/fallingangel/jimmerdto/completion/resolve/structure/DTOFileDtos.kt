package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoName
import net.fallingangel.jimmerdto.psi.DTOFile

class DTOFileDtos : Structure<DTOFile, List<String>> {
    /**
     * @param element DTO文件
     *
     * @return DTO文件中，定义的DTO列表
     */
    override fun value(element: DTOFile): List<String> {
        val dtos = PsiTreeUtil.getChildrenOfTypeAsList(element, DTODto::class.java)
        return dtos.map(DTODto::getDtoName).map(DTODtoName::getText)
    }
}
