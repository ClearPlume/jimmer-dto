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
    // 缩进体
    private val parents = TokenSet.create(DTO_BODY, ALIAS_GROUP_BODY, ENUM_BODY)
    // 父级为缩进体，但本身不需要缩进
    private val parentSymbols = TokenSet.create(BRACE_L, BRACE_R, ARROW)

    override fun getIndent(): Indent? {
        if (node.treeParent?.elementType in parents && node.elementType !in parentSymbols) {
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