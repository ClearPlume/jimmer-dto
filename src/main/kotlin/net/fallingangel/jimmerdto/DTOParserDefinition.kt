package net.fallingangel.jimmerdto

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.parser.DTOParser
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOTokenTypes
import net.fallingangel.jimmerdto.psi.DTOTypes

class DTOParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = DTOLexerAdapter()

    override fun createParser(project: Project) = DTOParser()

    override fun getFileNodeType() = Companion.FILE

    override fun getCommentTokens() = TokenSet.create(
        DTOTokenTypes.LINE_COMMENT,
        DTOTokenTypes.BLOCK_COMMENT,
        DTOTokenTypes.DOC_COMMENT
    )

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(DTOTypes.STRING, DTOTypes.SQL_STRING)

    override fun createElement(node: ASTNode): PsiElement = DTOTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) = DTOFile(viewProvider)

    object Companion {
        val FILE = IFileElementType(DTOLanguage)
    }
}
