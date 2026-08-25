package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.ImportEntry

interface DTOImportStatement : DTOElement {
    val qualifiedName: DTOQualifiedName

    val alias: DTOAlias?

    val groupedImport: DTOGroupedImport?

    /**
     * [groupedImport] 非空时不可使用
     */
    val simpleName: String
        get() = alias?.value ?: qualifiedName.simpleName

    val importEntries: List<ImportEntry>
        get() {
            val groupedImport = groupedImport
            return groupedImport?.types
                ?.mapNotNull {
                    val importedType = it.type.value ?: return@mapNotNull null
                    ImportEntry(qualifiedName.value + '.' + importedType, it.alias, it)
                }
                ?: listOf(ImportEntry(qualifiedName.value, alias, this))
        }
}
