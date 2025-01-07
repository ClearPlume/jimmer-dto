package net.fallingangel.jimmerdto.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.psi.DTOTypes.*

class DTOBlock(
    private val spacingBuilder: SpacingBuilder,
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?
) : AbstractBlock(node, wrap, alignment) {
    private val braces = TokenSet.create(BRACE_L, BRACE_R)

    override fun getIndent(): Indent? {
        if (node.treeParent == null) {
            return Indent.getNoneIndent()
        }
        if (node.treeParent.elementType == DTO_BODY && node.elementType !in braces) {
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf() = myNode.firstChildNode == null

    override fun buildChildren(): List<DTOBlock> {
        val whetherParent = node.elementType == DTO_BODY
        return generateSequence(myNode::getFirstChildNode, ASTNode::getTreeNext)
                .filter { it.elementType != TokenType.WHITE_SPACE }
                .map {
                    DTOBlock(
                        spacingBuilder,
                        it,
                        wrap,
                        if (whetherParent && it.elementType !in braces) Alignment.createAlignment() else null
                    )
                }
                .toList()
    }
}