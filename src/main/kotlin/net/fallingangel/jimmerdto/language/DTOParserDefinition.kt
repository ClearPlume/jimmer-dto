package net.fallingangel.jimmerdto.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.language.parser.DTOParser
import net.fallingangel.jimmerdto.language.psi.DTOFile
import net.fallingangel.jimmerdto.language.psi.DTOTypes
import net.fallingangel.jimmerdto.language.psi.DTOTokenSets

class DTOParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = DTOLexerAdapter()

    override fun createParser(project: Project) = DTOParser()

    override fun getFileNodeType() = FILE

    override fun getCommentTokens() = DTOTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = DTOTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) = DTOFile(viewProvider)

    @Suppress("CompanionObjectInExtension")
    companion object {
        val FILE = IFileElementType(DTOLanguage.INSTANCE)
    }
}