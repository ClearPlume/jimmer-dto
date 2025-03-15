package net.fallingangel.jimmerdto.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTODtoBody
import net.fallingangel.jimmerdto.util.*

class InsertEntityPropAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val caret = editor.caretModel.currentCaret
        val offset = caret.offset

        val dtoFile = e.getData(CommonDataKeys.PSI_FILE) as? DTOFile ?: return
        val space = dtoFile.findElementAt(offset) as? PsiWhiteSpace ?: return
        val body = space.parentOfType<DTODtoBody>() ?: return

        val negativeProps = body.negativeProps.mapNotNull { it.name?.value }
        val positiveProps = body.positiveProps.map { it.name.value }
        val functionProps = body.positiveProps
                .mapNotNull { it.arg }
                .map { it.values.map { value -> value.text } }
                .flatten()
        val aliasProps = body.aliasGroups
                .map {
                    it.positiveProps
                            .map { prop ->
                                prop.name.value
                            }
                }
                .flatten()
        val oldProps = negativeProps + positiveProps + functionProps + aliasProps

        val propPath = space.propPath()
        val props = dtoFile.clazz.walk(propPath).allProperties.map(LProperty<*>::name).filter { it !in oldProps }

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(
                offset,
                props.joinToString("\n")
            )
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = false

        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.project
        val file = e.getData(CommonDataKeys.PSI_FILE)

        if (editor == null || project == null || file == null || file.fileType != DTOFileType.INSTANCE) {
            return
        }

        val caret = editor.caretModel.currentCaret
        val selection = editor.selectionModel
        val elementAtCaret = file.findElementAt(caret.offset) ?: return

        if (!elementAtCaret.haveParent<DTODtoBody>()) {
            return
        }

        if (selection.selectedText != null) {
            return
        }

        // 光标之前应该有至少一个空格
        if (file.findElementAt(caret.offset - 1) !is PsiWhiteSpace) {
            return
        }

        e.presentation.isVisible = true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}