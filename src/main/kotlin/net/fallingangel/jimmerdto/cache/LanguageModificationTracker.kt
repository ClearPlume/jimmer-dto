package net.fallingangel.jimmerdto.cache

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.DTOPluginDisposable
import net.fallingangel.jimmerdto.util.hasAnnotation
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

class LanguageModificationTracker(private val project: Project, fileType: FileType) : ModificationTracker {
    private var modificationCount: Long = 0

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document)
                if (psiFile != null && psiFile.fileType === fileType) {
                    val editingEntity = when (psiFile) {
                        is PsiJavaFile -> {
                            psiFile.classes.any { it.isInterface && it.hasAnnotation(Constant.Annotation.ENTITY) }
                        }

                        is KtFile -> {
                            psiFile.declarations
                                    .filterIsInstance<KtClass>()
                                    .any { it.isInterface() && it.hasAnnotation(Constant.Annotation.ENTITY) }
                        }

                        else -> false
                    }
                    if (editingEntity) {
                        modificationCount++
                    }
                }
            }
        }, DTOPluginDisposable.getInstance(project))
    }

    override fun getModificationCount() = modificationCount
}