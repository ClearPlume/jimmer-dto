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
    private val parents = TokenSet.create(DTO_BODY, ALIAS_GROUP_BODY)

    override fun getIndent(): Indent? {
        if (node.treeParent?.elementType in parents && node.elementType !in braces) {
            return Indent.getNormalIndent()
        }
        if (
            node.treeParent?.elementType == EXPORT &&
            (node.treePrev?.elementType == TokenType.WHITE_SPACE && node.treePrev?.treePrev?.elementType == QUALIFIED_TYPE ||
                    node.treePrev?.elementType == QUALIFIED_TYPE)
        ) {
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf() = myNode.firstChildNode == null

    override fun buildChildren(): List<DTOBlock> {
        return generateSequence(myNode::getFirstChildNode, ASTNode::getTreeNext)
                .filter { it.elementType != TokenType.WHITE_SPACE }
                .map { DTOBlock(spacingBuilder, it, wrap, null) }
                .toList()
    }

    override fun getChildIndent(): Indent? {
        if (node.elementType in parents) {
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }
}