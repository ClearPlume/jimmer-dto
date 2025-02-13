package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBList
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.DTOMacro
import net.fallingangel.jimmerdto.psi.createMacroArg
import net.fallingangel.jimmerdto.util.get

class InsertMacroArg(private val element: DTOMacro) : BaseFix() {
    override fun getText() = "Insert macro arg"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val macroAvailableArgs = element[StructureType.MacroTypes]
        val argsHolder = JBList(macroAvailableArgs)

        val argChooser = PopupChooserBuilder(argsHolder)
                .setTitle("Macro Arg to Import")
                .createPopup()
        argChooser.showInBestPositionFor(editor)
        argChooser.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                if (event.isOk) {
                    val selectedArg = argsHolder.selectedValue
                    val arg = project.createMacroArg(selectedArg)

                    WriteCommandAction.runWriteCommandAction(project) {
                        element.node.addChild(arg.node, element.node.lastChildNode)
                    }
                }
            }
        })
    }
}