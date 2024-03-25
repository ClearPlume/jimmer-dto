package net.fallingangel.jimmerdto.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOPluginDisposable
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.util.classFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class StartupServices : StartupActivity.Background {
    override fun runActivity(project: Project) {
        VirtualFileManager.getInstance()
                .addAsyncFileListener(
                    { events ->
                        object : AsyncFileListener.ChangeApplier {
                            override fun afterVfsChange() {
                                events.forEach { event ->
                                    val isSave = event.isFromSave
                                    if (isSave) {
                                        val psiFile = event.file?.toPsiFile(project) ?: return@forEach
                                        val type = psiFile.fileType

                                        when (type) {
                                            is DTOFileType -> {
                                                psiFile.dtoFileChanged(project)
                                            }

                                            else -> return@forEach
                                        }
                                    }
                                }
                            }
                        }
                    },
                    DTOPluginDisposable.getInstance(project)
                )
    }

    private fun PsiFile.dtoFileChanged(project: Project) {
        PsiTreeUtil.getChildrenOfTypeAsList(this, DTODto::class.java)
                .forEach dto@{ dto ->
                    val dtoClassFile = dto.classFile() ?: return@dto
                    WriteCommandAction.runWriteCommandAction(project) {
                        dtoClassFile.delete(this@StartupServices)
                    }
                }
    }
}
