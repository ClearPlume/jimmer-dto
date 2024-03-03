package net.fallingangel.jimmerdto.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.util.classFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class StartupServices : StartupActivity.Background {
    override fun runActivity(project: Project) {
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    events.forEach { event ->
                        val isSave = event.isFromSave
                        if (isSave) {
                            val dtoFile = event.file ?: return@forEach
                            val type = dtoFile.fileType

                            if (type is DTOFileType) {
                                PsiTreeUtil.getChildrenOfTypeAsList(dtoFile.toPsiFile(project), DTODto::class.java)
                                        .forEach dto@{ dto ->
                                            val dtoClassFile = dto.classFile() ?: return@dto
                                            WriteCommandAction.runWriteCommandAction(project) {
                                                dtoClassFile.delete(this@StartupServices)
                                            }
                                        }
                            }
                        }
                    }
                }
            })
    }
}