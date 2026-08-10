package net.fallingangel.jimmerdto.action

import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.index.DTO_ENTITY_INDEX
import net.fallingangel.jimmerdto.lsi.jimmer.isEntity
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.project.sourceroot.DtoSourceRootType
import net.fallingangel.jimmerdto.project.sourceroot.dtoSourceRoots
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.notification
import net.fallingangel.jimmerdto.util.open
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class OpenDtoFileAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val entityElement = process(selectedElement.containingFile) { topLevelClasses() }?.singleOrNull() ?: return
        val entityQualifiedName = process(entityElement) { classQualifiedName() } ?: return

        val dtoFiles = FileBasedIndex.getInstance()
            .getContainingFiles(DTO_ENTITY_INDEX, entityQualifiedName, project.projectScope())
            .mapNotNull { it.toPsiFile(project) }

        if (dtoFiles.size == 1) {
            dtoFiles.single().navigate(true)
            return
        }

        if (dtoFiles.size > 1) {
            val point = JBPopupFactory.getInstance().guessBestPopupLocation(event.dataContext)
            PsiTargetNavigator(dtoFiles)
                .builderConsumer { builder ->
                    builder.setNamerForFiltering {
                        val file = it.dereference() as DTOFile
                        file.name
                    }
                }
                .navigate(point, "Choose DTO", project) {
                    it.navigate(true)
                    true
                }
            return
        }

        // 不存在已有 dto 文件时
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
        val vFile = entityElement.containingFile?.virtualFile ?: return
        val module = fileIndex.getModuleForFile(vFile) ?: return
        val rootType = if (fileIndex.isInTestSourceContent(vFile)) {
            DtoSourceRootType.TEST_SOURCE
        } else {
            DtoSourceRootType.SOURCE
        }

        val dtoRoots = module.dtoSourceRoots(rootType)
        when (dtoRoots.size) {
            0 -> project.notification("No DTO source root in module '${module.name}'", NotificationType.WARNING)

            1 -> {
                WriteCommandAction.runWriteCommandAction(project) {
                    dtoRoots.single().findOrCreateFile(dtoFileName).open(project)
                }
            }

            else -> {
                val pathByRoot = dtoRoots.associateWith { root ->
                    val contentRoot = fileIndex.getContentRootForFile(root)!!
                    VfsUtilCore.getRelativePath(root, contentRoot)
                }
                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(dtoRoots)
                    .setTitle("Choose DTO Source Root")
                    .setRenderer(SimpleListCellRenderer.create { label, root, _ ->
                        label.text = pathByRoot[root]
                    })
                    .setNamerForFiltering { pathByRoot[it] }
                    .setItemChosenCallback { root ->
                        WriteCommandAction.runWriteCommandAction(project) {
                            root.findOrCreateFile(dtoFileName).open(project)
                        }
                    }
                    .createPopup()
                    .show(JBPopupFactory.getInstance().guessBestPopupLocation(event.dataContext))
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = false
        val selectedElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val classes = process(selectedElement.containingFile ?: return) { topLevelClasses() } ?: return

        event.presentation.isVisible = classes.filter { process(it) { isEntity() } == true }.size == 1
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
