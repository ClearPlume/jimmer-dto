package net.fallingangel.jimmerdto.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering

class CreateJimmerDtoFile : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!

        NotificationGroupManager.getInstance()
                .getNotificationGroup("JimmerDTO Notification Group")
                .createNotification(
                    "Since the Dto language supports 'export', this operation is obsolete and will be removed in 0.0.8, so don't use it anymore!",
                    NotificationType.WARNING
                )
                .notify(project)

        val entityFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val qualifiedEntityName = entityFile.nameIdentifier(project)?.qualifiedClassNameForRendering() ?: return
        val entityPackage = qualifiedEntityName.substringBeforeLast('.', "")
        val entityName = qualifiedEntityName.substringAfterLast('.')
        val dtoFileName = "$entityPackage/$entityName.dto"

        val dtoRoot = entityFile.toPsiFile(project)?.originalElement?.let { dtoRoot(it) } ?: return
        val dtoFile = dtoRoot.findFile(dtoFileName)

        if (dtoFile != null) {
            dtoFile.open(project)
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val dtoDir = dtoRoot.findOrCreateDirectory(entityPackage.replace('.', '/'))
            dtoDir.createChildData(project, "$entityName.dto").open(project)
        }
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
}
