package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.element.DTOPropConfig
import net.fallingangel.jimmerdto.util.haveParent

class DTOTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {
        if (c != '<' || file !is DTOFile) {
            return Result.CONTINUE
        }
        val offset = editor.caretModel.offset
        val leaf = generateSequence(file.findElementAt(offset - 1)) { PsiTreeUtil.prevLeaf(it, true) }
            .firstOrNull { it !is PsiWhiteSpace }
            ?: return Result.CONTINUE
        if (leaf.elementType != DTOLanguage.token[DTOLexer.Identifier] || leaf.haveParent<DTOPropConfig>()) {
            return Result.CONTINUE
        }
        editor.document.insertString(offset, "<>")
        editor.caretModel.moveToOffset(offset + 1)
        return Result.STOP
    }
}
