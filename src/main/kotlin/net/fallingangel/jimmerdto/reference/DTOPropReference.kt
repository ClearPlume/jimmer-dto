package net.fallingangel.jimmerdto.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import icons.Icons
import net.fallingangel.jimmerdto.psi.impl.DTOPsiImplUtil


class DTOPropReference(private val element: PsiElement, range: TextRange) : PsiReferenceBase<PsiElement>(element, range), PsiPolyVariantReference {
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return results.getOrNull(0)?.element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return DTOPsiImplUtil.findDTOs(element)
                .map { PsiElementResolveResult(it) }
                .toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        return DTOPsiImplUtil.findDTOs(element)
                .filter { it.dtoName.text.isNotBlank() }
                .map {
                    LookupElementBuilder
                            .create(it)
                            .withIcon(Icons.icon_16)
                            .withTypeText(it.containingFile.name)
                }
                .toTypedArray()
    }
}
