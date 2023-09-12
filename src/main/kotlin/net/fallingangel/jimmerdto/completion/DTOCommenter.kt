package net.fallingangel.jimmerdto.completion

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.psi.DTOTokenTypes

class DTOCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentPrefix() = "//"

    override fun getLineCommentTokenType(): IElementType = DTOTokenTypes.LINE_COMMENT

    override fun getBlockCommentPrefix() = "/*"

    override fun getBlockCommentSuffix() = "*/"

    override fun getCommentedBlockCommentPrefix() = "*"

    override fun getCommentedBlockCommentSuffix() = ""

    override fun getBlockCommentTokenType(): IElementType = DTOTokenTypes.BLOCK_COMMENT

    override fun getDocumentationCommentPrefix() = "/**"

    override fun getDocumentationCommentLinePrefix() = "*"

    override fun getDocumentationCommentSuffix() = "*/"

    override fun getDocumentationCommentTokenType(): IElementType = DTOTokenTypes.DOC_COMMENT

    override fun isDocumentationComment(element: PsiComment) = element.tokenType == DTOTokenTypes.DOC_COMMENT
}
