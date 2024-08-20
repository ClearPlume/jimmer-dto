package net.fallingangel.jimmerdto.action

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.DTODtoBody
import net.fallingangel.jimmerdto.psi.DTOExport
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class InsertEntityPropAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val caret = editor.caretModel.currentCaret
        val offset = caret.offset

        val dtoFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val space = dtoFile.findElementAt(offset) as? PsiWhiteSpace ?: return
        val export = dtoFile.getChildOfType<DTOExport>()

        val body = space.parentOfType<DTODtoBody>() ?: return

        val negativeProps = body.explicitPropList.mapNotNull { it.negativeProp }.map { it.propName.text }
        val positiveProps = body.explicitPropList.mapNotNull { it.positiveProp }.map { it.propName.text }
        val functionProps = body.explicitPropList
                .mapNotNull { it.positiveProp?.propArgs }
                .map { it.valueList.map { value -> value.text } }
                .flatten()
        val aliasProps = body.explicitPropList
                .mapNotNull { it.aliasGroup }
                .map {
                    it.aliasGroupBody
                            .positivePropList
                            .map { prop ->
                                prop.propName.text
                            }
                }
                .flatten()
        val oldProps = negativeProps + positiveProps + functionProps + aliasProps

        val propPath = if (space.haveUpper) {
            space.upper.propPath()
        } else {
            space.propPath()
        }

        val entityName = if (export != null) {
            export.qualifiedType.text
        } else {
            val dtoRoot = dtoRoot(dtoFile)?.path ?: return
            val dtoPath = dtoFile.virtualFile.parent.path
            val `package` = dtoPath.substringAfter("$dtoRoot/").replace('/', '.')
            "${`package`}.${dtoFile.name.removeSuffix(".dto")}"
        }
        val entity = JavaPsiFacade.getInstance(project).findClass(entityName, ProjectScope.getProjectScope(project)) ?: return

        val props = when (entity.language) {
            is JavaLanguage -> entity.virtualFile
                    .psiClass(project, propPath)
                    ?.methods()
                    ?.filter { it.name !in oldProps }
                    ?.map { it.name }

            is KotlinLanguage -> entity.virtualFile
                    .ktClass(project, propPath)
                    ?.properties()
                    ?.filter { it.name !in oldProps }
                    ?.map { it.name }

            else -> throw UnsupportedLanguageException("${entity.language} is unsupported")
        } ?: emptyList()

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