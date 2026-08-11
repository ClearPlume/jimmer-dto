package net.fallingangel.jimmerdto

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.util.findChildren
import java.util.concurrent.Callable

class DTOClassCleaner : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = DTOPluginDisposable.getInstance(project)

        VirtualFileManager.getInstance()
            .addAsyncFileListener(
                { events ->
                    val dtoFiles = events.asSequence()
                        .filter(VFileEvent::isFromSave)
                        .mapNotNull(VFileEvent::getFile)
                        .filter { FileTypeRegistry.getInstance().getFileTypeByFileName(it.nameSequence) is DTOFileType }
                        .distinct()
                        .toList()

                    if (dtoFiles.isEmpty()) return@addAsyncFileListener null

                    object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            scheduleCleanup(project, disposable, dtoFiles)
                        }
                    }
                },
                disposable
            )
    }

    private fun scheduleCleanup(project: Project, disposable: Disposable, dtoFiles: List<VirtualFile>) {
        ReadAction.nonBlocking(Callable { collectGenerateClasses(project, dtoFiles) })
            .inSmartMode(project)
            .expireWith(disposable)
            .coalesceBy(this, project)
            .finishOnUiThread(ModalityState.nonModal()) { generated ->
                if (generated.isEmpty()) return@finishOnUiThread

                WriteAction.run<Exception> {
                    generated
                        .filter(VirtualFile::isValid)
                        .forEach { it.delete(this) }
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun collectGenerateClasses(project: Project, dtoFiles: List<VirtualFile>): List<VirtualFile> {
        val psiManager = PsiManager.getInstance(project)
        return dtoFiles.asSequence()
            .filter(VirtualFile::isValid)
            .mapNotNull(psiManager::findFile)
            .flatMap { it.findChildren<DTODto>("/dtoFile/dto") }
            .mapNotNull { it.name.resolve()?.containingFile?.virtualFile }
            .filter(VirtualFile::isValid)
            .distinct()
            .toList()
    }
}
