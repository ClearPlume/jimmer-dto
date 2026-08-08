package net.fallingangel.jimmerdto.refactor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

class DTORefactoringSupport : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is DTONamedElement && (element.parent as? DTOPositiveProp)?.arg == null
    }
}