package net.fallingangel.jimmerdto.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findOrCreateFile
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.util.open

class CreateJimmerDtoFile : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val entityElement = process(selectedElement.containingFile) { topLevelClasses() }?.singleOrNull() ?: return

        val entityQualifiedName = process(entityElement) { classQualifiedName() } ?: return
        val entityPackage = entityQualifiedName.substringBeforeLast('.', "")
        val entityName = entityQualifiedName.substringAfterLast('.')
        val dtoFileName = buildString {
            if (entityPackage.isNotEmpty()) {
                append(entityPackage.replace('.', '/'))
                append('/')
            }
            append("$entityName.dto")
        }

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val sourceRoot = entityElement.containingFile?.virtualFile?.let { fileIndex.getSourceRootForFile(it) }
        val dtoRoot = sourceRoot?.parent?.findDirectory("dto") ?: return
        val dtoFile = dtoRoot.findFile(dtoFileName)

        if (dtoFile != null) {
            dtoFile.open(project)
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            dtoRoot.findOrCreateFile(dtoFileName).open(project)
        }
    }

    override fun update(event: AnActionEvent) {
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val classes = process(selectedElement.containingFile) { topLevelClasses() } ?: return

        event.presentation.isVisible = classes.filter {
            process(it) {
                kind() == LKind.Interface && hasAnnotation(JimmerAnnotations.Entity)
            } == true
        }.size == 1
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
