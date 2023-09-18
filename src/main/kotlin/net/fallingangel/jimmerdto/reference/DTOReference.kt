package net.fallingangel.jimmerdto.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.psi.impl.DTOPsiImplUtil


class DTOReference(private val element: PsiElement, range: TextRange) : PsiReferenceBase<PsiElement>(element, range), PsiPolyVariantReference {
    private val name: String

    init {
        name = element.text.substring(range.startOffset, range.endOffset)
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isEmpty()) {
            null
        } else {
            results[0].element
        }
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return DTOPsiImplUtil.findDTOs(element)
                .map { PsiElementResolveResult(it) }
                .toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        return DTOPsiImplUtil.findDTOs(element)
                .filter { it.dtoName.name.isNotBlank() }
                .map {
                    LookupElementBuilder
                            .create(it)
                            .withIcon(Constant.ICON)
                            .withTypeText(it.containingFile.name)
                }
                .toTypedArray()
    }
}
