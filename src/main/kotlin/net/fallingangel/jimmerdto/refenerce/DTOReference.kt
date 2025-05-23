package net.fallingangel.jimmerdto.refenerce

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

class DTOReference(private val element: DTONamedElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        if (DumbService.getInstance(element.project).isDumb) {
            return null
        }
        return element.resolve()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }
}