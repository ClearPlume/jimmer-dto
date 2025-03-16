package net.fallingangel.jimmerdto.refenerce

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

class DTOReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(DTONamedElement::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    element as DTONamedElement
                    return arrayOf(DTOReference(element, element.firstChild.textRangeInParent))
                }
            },
        )
    }
}