package net.fallingangel.jimmerdto.completion

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOParser

class DTOCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentPrefix() = "//"

    override fun getLineCommentTokenType() = DTOLanguage.token[DTOParser.LineComment]

    override fun getBlockCommentPrefix() = "/*"

    override fun getBlockCommentSuffix() = "*/"

    override fun getCommentedBlockCommentPrefix() = "*"

    override fun getCommentedBlockCommentSuffix() = ""

    override fun getBlockCommentTokenType() = DTOLanguage.token[DTOParser.BlockComment]

    override fun getDocumentationCommentPrefix() = "/**"

    override fun getDocumentationCommentLinePrefix() = "*"

    override fun getDocumentationCommentSuffix() = "*/"

    override fun getDocumentationCommentTokenType() = DTOLanguage.token[DTOParser.DocComment]

    override fun isDocumentationComment(element: PsiComment) = element.tokenType == DTOLanguage.token[DTOParser.DocComment]
}
