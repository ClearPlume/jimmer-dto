package net.fallingangel.jimmerdto.highlighting

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.*

class DTOFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val bodyTypes = arrayOf(
            DTOGroupedTypes::class.java,
            DTODtoBody::class.java,
            DTOPropBody::class.java,
            DTOAliasGroupBody::class.java,
            DTOEnumBody::class.java,
        )
        return PsiTreeUtil.findChildrenOfAnyType(root, *bodyTypes)
                .map { FoldingDescriptor(it.node, it.textRange) }
                .toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = "{...}"

    override fun isCollapsedByDefault(node: ASTNode) = false
}