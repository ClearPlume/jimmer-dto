package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.ui.components.JBList
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createImport
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable

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
        val classesHolder = JBList(classes)

        val classChooser = PopupChooserBuilder(classesHolder)
                .setTitle("Class to Import")
                .createPopup()
        classChooser.showInBestPositionFor(editor)
        classChooser.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                if (event.isOk) {
                    val dtoFile = file.findChild<PsiElement>("/dtoFile")
                    val selectedClass = classesHolder.selectedValue
                    val dtoImport = project.createImport(selectedClass)

                    val import = file.findChildNullable<DTOImportStatement>("/dtoFile/importStatement")
                    if (import != null) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            dtoFile.node.addChild(dtoImport.node, import.node)
                            dtoFile.node.addLeaf(TokenType.WHITE_SPACE, "\n", import.node)
                        }
                    } else {
                        val dto = file.findChild<DTODto>("/dtoFile/dto")
                        WriteCommandAction.runWriteCommandAction(project) {
                            dtoFile.node.addChild(dtoImport.node, dto.node)
                            dtoFile.node.addLeaf(TokenType.WHITE_SPACE, "\n\n", dto.node)
                        }
                    }
                }
            }
        })
    }
}