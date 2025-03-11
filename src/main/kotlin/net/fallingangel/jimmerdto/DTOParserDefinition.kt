package net.fallingangel.jimmerdto

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.impl.DTOExportStatementImpl
import net.fallingangel.jimmerdto.psi.element.impl.DTOQualifiedNameImpl
import net.fallingangel.jimmerdto.psi.element.impl.DTOQualifiedNamePartImpl
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = DTOLexerAdapter()

    override fun createParser(project: Project) = DTOParserAdapter()

    override fun getFileNodeType() = Companion.FILE

    override fun getWhitespaceTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.WhiteSpace,
        )
    }

    override fun getCommentTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.LineComment,
            DTOLexer.BlockComment,
            DTOLexer.DocComment,
        )
    }

    override fun getStringLiteralElements(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.StringLiteral,
            DTOLexer.SqlStringLiteral,
        )
    }

    override fun createElement(node: ASTNode): PsiElement {
        val type = node.elementType

        if (type is TokenIElementType || type !is RuleIElementType) {
            return ANTLRPsiNode(node)
        }

        return when (type.ruleIndex) {
            DTOParser.RULE_exportStatement -> DTOExportStatementImpl(node)
            DTOParser.RULE_qualifiedName -> DTOQualifiedNameImpl(node)
            DTOParser.RULE_qualifiedNamePart -> DTOQualifiedNamePartImpl(node)
            else -> ANTLRPsiNode(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider) = DTOFile(viewProvider)

    object Companion {
        val FILE = IFileElementType(DTOLanguage)
    }
}
