package net.fallingangel.jimmerdto.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDirectory
import icons.Icons
import net.fallingangel.jimmerdto.project.sourceroot.DtoSourceRootType

class CreateFileFromTemplateAction : CreateFileFromTemplateAction(), DumbAware {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Jimmer DTO File").addKind("DTO file", Icons.PluginIcon, "Jimmer DTO File")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
        return "Jimmer DTO File"
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        val view = LangDataKeys.IDE_VIEW.getData(dataContext) ?: return false
        val index = ProjectFileIndex.getInstance(project)
        return view.directories.any {
            index.isUnderSourceRootOfType(it.virtualFile, setOf(DtoSourceRootType.SOURCE, DtoSourceRootType.TEST_SOURCE))
        }
    }
}