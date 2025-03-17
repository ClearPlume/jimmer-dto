package net.fallingangel.jimmerdto.refactor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOAlias

class DTORefactoringSupport : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is DTOAlias
    }
}