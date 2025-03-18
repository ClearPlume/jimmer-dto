package net.fallingangel.jimmerdto.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOPluginDisposable
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.util.findChildren
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
        findChildren<DTODto>("/dtoFile/dto")
                .forEach dto@{ dto ->
                    val dtoClass = dto.name.resolve() ?: return@dto
                    val dtoClassFile = dtoClass.containingFile.virtualFile
                    WriteCommandAction.runWriteCommandAction(project) {
                        dtoClassFile.delete(this@StartupServices)
                    }
                }
    }
}
