package net.fallingangel.jimmerdto.psi.resolve

import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

class ImportEntry(val qualifiedName: String, val alias: DTOAlias?, val declaration: DTOElement) {
    val simpleName = alias?.value ?: qualifiedName.substringAfterLast('.')

    val target: PsiNamedElement?
        get() = when (declaration) {
            is DTOImportStatement -> declaration.qualifiedName.target?.source?.let { alias ?: it }
            is DTOImportedType -> declaration.type.target?.source?.let { alias ?: it }
            else -> null
        }
}
