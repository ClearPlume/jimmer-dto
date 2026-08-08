package net.fallingangel.jimmerdto.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile

class DTOEnterHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?,
    ): Result {
        if (file !is DTOFile) {
            return Result.Continue
        }

        val document = editor.document
        val offset = caretOffset.get()
        val chars = document.charsSequence
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))

        val head = chars.subSequence(lineStart, offset).toString()
        val trimmed = head.trimStart()
        val indent = head.substring(0, head.length - trimmed.length)

        val isDoc = trimmed.startsWith("/**")
        if (!isDoc && !trimmed.startsWith("/*")) {
            return Result.Continue
        }

        if (trimmed.contains("*/") || isTerminated(chars, offset)) {
            return Result.Continue
        }

        val insertion = if (isDoc) "\n$indent * \n$indent */" else "\n$indent \n$indent */"
        document.insertString(offset, insertion)
        editor.caretModel.moveToOffset(offset + 1 + indent.length + if (isDoc) 3 else 1)
        return Result.Stop
    }

    private fun isTerminated(chars: CharSequence, offset: Int): Boolean {
        val close = chars.indexOf("*/", offset)
        if (close == -1) {
            return false
        }
        val open = chars.indexOf("/*", offset)
        return open == -1 || close < open
    }
}