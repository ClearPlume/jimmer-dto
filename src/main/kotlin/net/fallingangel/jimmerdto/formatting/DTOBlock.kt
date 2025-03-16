package net.fallingangel.jimmerdto.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.DTOLanguage.rule
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOParser.*

class DTOBlock(
    private val spacingBuilder: SpacingBuilder,
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
) : AbstractBlock(node, wrap, alignment) {
    // 缩进体
    private val parents = DTOLanguage.ruleSet(RULE_dtoBody, RULE_groupedImport, RULE_aliasGroupBody, RULE_enumBody)

    // 父级为缩进体，但本身不需要缩进
    private val parentSymbols = DTOLanguage.tokenSet(LBrace, RBrace, Arrow)

    override fun getIndent(): Indent? {
        if (node.treeParent?.elementType in parents && node.elementType !in parentSymbols) {
            return Indent.getNormalIndent()
        }
        if (node.elementType == token[Arrow] && node.treeParent?.elementType == rule[RULE_exportStatement]) {
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