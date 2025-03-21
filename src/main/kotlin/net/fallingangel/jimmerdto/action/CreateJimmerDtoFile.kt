package net.fallingangel.jimmerdto.action

import com.intellij.lang.java.JavaLanguage
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import net.fallingangel.jimmerdto.util.*
import org.babyfish.jimmer.sql.Entity
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class CreateJimmerDtoFile : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!

        project.notification(
            "Since the Dto language supports 'export', this operation is obsolete and will be removed in 0.0.8, so don't use it anymore!",
            NotificationType.WARNING
        )

        val entityClass = event.getData(CommonDataKeys.VIRTUAL_FILE)?.toPsiFile(project)?.getChildOfType<PsiClass>() ?: return
        val entityQualifiedName = entityClass.qualifiedName ?: return
        val entityPackage = entityQualifiedName.substringBeforeLast('.', "")
        val entityName = entityClass.name ?: return
        val dtoFileName = entityClass.qualifiedName?.replaceAfterLast('.', "dto") ?: return

        val dtoRoot = entityClass.contentRoot?.findDirectory("dto") ?: return
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
        val project = event.project ?: return
        val selectedFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (selectedFile.isDirectory) {
            event.presentation.isVisible = false
            return
        }
        val psiFile = selectedFile.toPsiFile(project) ?: return
        event.presentation.isVisible = when (psiFile.language) {
            JavaLanguage.INSTANCE -> {
                val clazz = psiFile.getChildOfType<PsiClass>() ?: return
                clazz.hasAnnotation(Entity::class)
            }

            KotlinLanguage.INSTANCE -> {
                val clazz = psiFile.getChildOfType<KtClass>() ?: return
                clazz.hasAnnotation(Entity::class)
            }

            else -> false
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
