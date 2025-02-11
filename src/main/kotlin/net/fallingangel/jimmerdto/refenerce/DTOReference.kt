package net.fallingangel.jimmerdto.refenerce

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.IncorrectOperationException
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.util.DTOPsiUtil

class DTOReference(private val element: DTONamedElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    override fun resolve() = +element

    override fun handleElementRename(newElementName: String): PsiElement {
        return when (val element = myElement as DTONamedElement) {
            is DTOPropName -> DTOPsiUtil.setName(element, newElementName)
            is DTOValue -> DTOPsiUtil.setName(element, newElementName)
            is DTOQualifiedNamePart -> DTOPsiUtil.setName(element, newElementName)
            is DTOEnumInstance -> DTOPsiUtil.setName(element, newElementName)
            is DTOMacroArg -> DTOPsiUtil.setName(element, newElementName)
            else -> throw IncorrectOperationException()
        }
    }
}