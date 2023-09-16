package net.fallingangel.jimmerdto.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import net.fallingangel.jimmerdto.annotations
import net.fallingangel.jimmerdto.dtoRoot
import net.fallingangel.jimmerdto.nameIdentifier
import net.fallingangel.jimmerdto.psiFile
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName

class CreateJimmerDtoFile : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!

        val entityFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val qualifiedEntityName = entityFile.nameIdentifier(project)?.kotlinFqName?.asString() ?: return
        val entityPackage = qualifiedEntityName.substringBeforeLast('.', "")
        val entityName = qualifiedEntityName.substringAfterLast('.')
        val dtoFileName = "$entityPackage/$entityName.dto"

        val dtoRoot = entityFile.psiFile(project)?.originalElement?.let { dtoRoot(it) } ?: return
        val dtoFile = dtoRoot.findFile(dtoFileName)

        if (dtoFile != null) {
            openFile(project, dtoFile)
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val dtoDir = dtoRoot.findOrCreateDirectory(entityPackage.replace('.', '/'))
            val createDtoFile = dtoDir.createChildData(project, "$entityName.dto")
            openFile(project, createDtoFile)
        }
    }

    private fun openFile(project: Project, file: VirtualFile) {
        val openFileDescriptor = OpenFileDescriptor(project, file, 0)
        FileEditorManager.getInstance(project).openEditor(openFileDescriptor, true)
    }

    override fun update(event: AnActionEvent) {
        val selectedFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (selectedFile.isDirectory) {
            event.presentation.isVisible = false
            return
        }
        val annotations = selectedFile.annotations(event.project!!)
        event.presentation.isVisible = "org.babyfish.jimmer.sql.Entity" in annotations
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
