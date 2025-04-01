package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiShortNamesCache
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createImport
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.popupChooser

class ImportClass(private val element: DTOQualifiedName) : BaseFix() {
    override fun getText() = "Import class"

    override fun isAvailable(): Boolean {
        return PsiShortNamesCache.getInstance(element.project)
                .allClassNames
                .contains(element.text)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val classes = PsiShortNamesCache.getInstance(project)
                .getClassesByName(element.text, ProjectScope.getAllScope(project))
                .mapNotNull { it.qualifiedName }
        val dtoFile = file.findChild<PsiElement>("/dtoFile")

        editor.popupChooser("Class to Import", classes) {
            val newImport = project.createImport(it)
            val oldImport = file.findChildNullable<DTOImportStatement>("/dtoFile/importStatement")

            WriteCommandAction.runWriteCommandAction(project) {
                dtoFile.addBefore(newImport, oldImport ?: file.findChild("/dtoFile/dto"))
            }
        }
    }
}