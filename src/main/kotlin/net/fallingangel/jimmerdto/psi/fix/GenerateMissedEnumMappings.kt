package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import net.fallingangel.jimmerdto.psi.DTOEnumBody
import net.fallingangel.jimmerdto.psi.createEnumMappings

/**
 * @property missedMappings 要添加的映射的名称，形如：[A, B, C]
 */
class GenerateMissedEnumMappings(
    private val propName: String,
    private val missedMappings: List<String>,
    private val enumBody: DTOEnumBody
) : BaseFix() {
    override fun getText() = if (missedMappings.size == 1) {
        "Generate missed mapping for $propName"
    } else {
        "Generate missed mappings for $propName"
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val lastChild = enumBody.node.lastChildNode
        project.createEnumMappings(missedMappings.map { "$it: \"DummyValueFor$it\"" })
                .map {
                    WriteCommandAction.runWriteCommandAction(project) {
                        enumBody.node.addChild(it.node, lastChild)
                        enumBody.node.addLeaf(TokenType.WHITE_SPACE, "\n", lastChild)
                    }
                }
    }
}