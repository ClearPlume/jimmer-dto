package net.fallingangel.jimmerdto.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.psi.DTOPositiveProp

class DTOReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar
                .registerReferenceProvider(
                    PlatformPatterns.psiElement(DTOPositiveProp::class.java),
                    object : PsiReferenceProvider() {
                        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                            return if (element is DTOPositiveProp) {
                                arrayOf(DTOPropReference(element, element.propName.textRange))
                            } else {
                                emptyArray()
                            }
                        }
                    }
                )
    }
}
