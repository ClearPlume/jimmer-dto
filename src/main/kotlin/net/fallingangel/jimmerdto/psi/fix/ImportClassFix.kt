package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import com.intellij.psi.search.PsiShortNamesCache
import net.fallingangel.jimmerdto.psi.demand
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.util.file

class ImportClassFix(part: DTOQualifiedNamePart) : HintAction {
    private val project = part.project
    private val scope = part.resolveScope
    private val name = part.part
    private val file = part.file
    private val pointer = part.createSmartPointer()

    private val relatedClasses by lazy { PsiShortNamesCache.getInstance(project).getClassesByName(name, scope) }

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return relatedClasses.isNotEmpty()
    }

    override fun showHint(editor: Editor): Boolean {
        val part = pointer.element ?: return false
        HintManager.getInstance()
            .showQuestionHint(editor, text, part.textOffset, part.textOffset + part.textLength) {
                importing(editor)
            }
        return true
    }

    override fun getFamilyName() = "Import"

    override fun getText(): String {
        return if (relatedClasses.size > 1) {
            "Import '${name}'? (multiple choices...)"
        } else {
            "Import '${relatedClasses[0].qualifiedName}'?"
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        importing(editor)
    }

    private fun importing(editor: Editor): Boolean {
        relatedClasses.singleOrNull()
            ?.let {
                WriteCommandAction.runWriteCommandAction(editor.project) {
                    file.addImport(it.demand(PsiClass::getQualifiedName))
                }
            }
            ?: run {
                PsiTargetNavigator(relatedClasses)
                    .navigate(editor, "Choose $name") {
                        WriteCommandAction.runWriteCommandAction(editor.project) {
                            file.addImport(it.demand(PsiClass::getQualifiedName))
                        }
                        true
                    }
            }
        return true
    }
}
