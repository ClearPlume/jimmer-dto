package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.element.DTOPropConfig
import net.fallingangel.jimmerdto.util.haveParent

class DTOBackspaceHandler : BackspaceHandlerDelegate() {
    private var deleteGt = false

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        deleteGt = c == '<' &&
                file is DTOFile &&
                file.findElementAt(editor.caretModel.offset)
                    ?.takeIf { it.elementType == DTOLanguage.token[DTOLexer.NotEquals2] }
                    ?.haveParent<DTOPropConfig>() == false
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (c != '<' || !deleteGt) {
            return false
        }
        val document = editor.document
        val offset = editor.caretModel.offset
        if (document.textLength <= offset) {
            return false
        }
        if (document.charsSequence[offset] == '>') {
            document.deleteString(offset, offset + 1)
        }
        return true
    }
}