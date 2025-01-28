package net.fallingangel.jimmerdto.refenerce

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.IncorrectOperationException
import net.fallingangel.jimmerdto.psi.DTONegativeProp
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.util.DTOPsiUtil

class DTOReference(private val element: DTONamedElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve() = +element

    override fun handleElementRename(newElementName: String): PsiElement {
        return when (val element = myElement as DTONamedElement) {
            is DTOPositiveProp -> DTOPsiUtil.setName(element, newElementName)
            is DTONegativeProp -> DTOPsiUtil.setName(element, newElementName)
            is DTOQualifiedNamePart -> DTOPsiUtil.setName(element, newElementName)
            else -> throw IncorrectOperationException()
        }
    }
}