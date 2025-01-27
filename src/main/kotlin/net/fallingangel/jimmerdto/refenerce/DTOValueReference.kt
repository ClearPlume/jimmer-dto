package net.fallingangel.jimmerdto.refenerce

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOValue
import net.fallingangel.jimmerdto.util.DTOPsiUtil
import net.fallingangel.jimmerdto.util.element
import net.fallingangel.jimmerdto.util.fqe

class DTOValueReference(private val value: DTOValue, element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        val project = value.project
        val propName = value.text
        val prop = value.parent.parent as DTOPositiveProp

        return JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project))?.element(propName)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return DTOPsiUtil.setName(value, newElementName)
    }
}