package net.fallingangel.jimmerdto.refactor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.DTODtoName

class DTORefactoringSupport : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is DTOAlias || element is DTOAnnotationParameter || element is DTODtoName
    }
}