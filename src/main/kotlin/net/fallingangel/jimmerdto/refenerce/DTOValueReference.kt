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
import net.fallingangel.jimmerdto.util.propPath

class DTOValueReference(private val value: DTOValue, element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve(): PsiElement? {
        val project = value.project
        val prop = value.parent.parent as DTOPositiveProp
        val clazz = JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project)) ?: return null
        val propPath = value.propPath()
        if (propPath.isEmpty()) {
            return null
        }
        return clazz.element(propPath)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return DTOPsiUtil.setName(value, newElementName)
    }
}