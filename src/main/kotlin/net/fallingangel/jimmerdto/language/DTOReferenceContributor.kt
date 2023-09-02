package net.fallingangel.jimmerdto.language

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.language.psi.DTODtoName
import net.fallingangel.jimmerdto.language.psi.DTOTypes

class DTOReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar
                .registerReferenceProvider(
                    PlatformPatterns.psiElement(DTOTypes.DTO_NAME),
                    object : PsiReferenceProvider() {
                        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                            return if (element is DTODtoName) {
                                arrayOf(DTOReference(element, element.identifier.textRange))
                            } else {
                                emptyArray()
                            }
                        }
                    }
                )
    }
}
