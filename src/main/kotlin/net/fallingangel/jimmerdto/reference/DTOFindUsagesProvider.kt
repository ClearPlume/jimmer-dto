package net.fallingangel.jimmerdto.reference

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOAlias

class DTOFindUsagesProvider : FindUsagesProvider {
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is DTOAlias
    }

    override fun getHelpId(psiElement: PsiElement) = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is DTOAlias -> "alias"
            else -> ""
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is DTOAlias -> {
                val hostName = element.hostName

                buildString {
                    append(element.value)

                    if (hostName != null) {
                        append(" for '$hostName'")
                    }
                }
            }

            else -> ""
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is DTOAlias -> if (useFullName) {
                getDescriptiveName(element)
            } else {
                element.value
            }

            else -> ""
        }
    }
}
