package net.fallingangel.jimmerdto.highlighting

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.lexer.RuleIElementType

class DTOFoldingBuilder : FoldingBuilderEx(), DumbAware {
    private val bodies = listOf(
        DTOParser.RULE_dtoBody,
        DTOParser.RULE_groupedImport,
        DTOParser.RULE_aliasGroupBody,
        DTOParser.RULE_propBody,
        DTOParser.RULE_enumBody,
    )

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        root.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val type = element.elementType
                if (type is RuleIElementType) {
                    if (type.ruleIndex in bodies) {
                        descriptors.add(FoldingDescriptor(element.node, element.textRange))
                    }
                }
                super.visitElement(element)
            }
        })
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode) = "{...}"

    override fun isCollapsedByDefault(node: ASTNode) = false
}