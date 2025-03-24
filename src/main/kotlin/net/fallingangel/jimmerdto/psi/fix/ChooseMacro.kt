package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBList
import net.fallingangel.jimmerdto.psi.element.createMacroName

class ChooseMacro(private val element: PsiElement) : BaseFix() {
    override fun getText() = "Choose new macro name"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val macrosHolder = JBList("allScalars", "allReferences")
        val macroChooser = PopupChooserBuilder(macrosHolder)
                .setTitle("Class to Import")
                .createPopup()
        macroChooser.showInBestPositionFor(editor)
        macroChooser.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                if (event.isOk) {
                    val newMacroName = project.createMacroName(macrosHolder.selectedValue)
                    WriteCommandAction.runWriteCommandAction(project) {
                        element.node.treeParent.replaceChild(element.node, newMacroName.node)
                    }
                }
            }
        })
    }
}