package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.ui.components.JBList
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOImportStatement
import net.fallingangel.jimmerdto.psi.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.createImport
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

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
                    val selectedClass = classesHolder.selectedValue
                    val dtoImport = project.createImport(selectedClass)

                    val import = file.getChildOfType<DTOImportStatement>()
                    if (import != null) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            file.node.addChild(dtoImport.node, import.node)
                            file.node.addLeaf(TokenType.WHITE_SPACE, "\n", import.node)
                        }
                    } else {
                        val dto = file.getChildOfType<DTODto>()!!
                        WriteCommandAction.runWriteCommandAction(project) {
                            file.node.addChild(dtoImport.node, dto.node)
                            file.node.addLeaf(TokenType.WHITE_SPACE, "\n\n", dto.node)
                        }
                    }
                }
            }
        })
    }
}