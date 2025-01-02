package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOImport

class DTOFileImports : Structure<DTOFile, List<String>> {
    /**
     * @param element DTO文件
     *
     * @return DTO文件中，导入的类型列表
     */
    override fun value(element: DTOFile): List<String> {
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(element, DTOImport::class.java)
        val importedTypes = imports
                .filter { it.qualifiedType.qualifiedTypeAlias == null && it.groupedTypes == null }
                .map { it.qualifiedType.qualifiedName.qualifiedNamePartList.last().text }
        val importedSingleAliasTypes = imports
                .filter { it.qualifiedType.qualifiedTypeAlias != null }
                .map { it.qualifiedType.qualifiedTypeAlias!!.identifier.text }
        val importedGroupedAliasTypes = imports
                .filter { it.groupedTypes != null }
                .map {
                    val groupedTypes = it.groupedTypes!!.qualifiedTypeList
                    val alias = groupedTypes
                            .filter { type -> type.qualifiedTypeAlias != null }
                            .map { type -> type.qualifiedTypeAlias!!.identifier.text }
                    val types = groupedTypes
                            .filter { type -> type.qualifiedTypeAlias == null }
                            .map { type -> type.lastChild.text }
                    alias + types
                }
                .flatten()
        return importedTypes + importedSingleAliasTypes + importedGroupedAliasTypes
    }
}
