package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropName
import net.fallingangel.jimmerdto.util.get
import org.jetbrains.kotlin.psi.psiUtil.endOffset

@Suppress("UnstableApiUsage")
class DTOInlayHintProvider : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("NoSettings")
    override val name: String
        get() = "JimmerDTOPropNullabilityHint"
    override val previewText: String
        get() = ""

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        throw NotImplementedError("No need implementation")
    }

    override fun createSettings() = NoSettings()

    override fun isLanguageSupported(language: Language) = language == DTOLanguage.INSTANCE

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        if (file is DTOFile) {
            return object : FactoryInlayHintsCollector(editor) {
                override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                    if (element.project.isDefault || !element.isValid) {
                        return false
                    }
                    if (element !is DTOPropName) {
                        return true
                    }

                    val elementProp = element.parent as DTOPositiveProp
                    // 不是方法才走这个逻辑
                    val propArgs = elementProp.propArgs
                    if (propArgs == null) {
                        val properties = element[StructureType.PropProperties]
                        val prop = properties.find { it.name == element.text } ?: return false
                        if (prop.nullable) {
                            sink.addInlineElement(
                                element.endOffset,
                                true,
                                factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                                false
                            )
                        }
                    } else if (element.text in arrayOf("flat", "id")) {
                        val properties = element[StructureType.PropProperties]
                        val prop = properties.find { it.name == propArgs.value.text } ?: return false
                        if (prop.nullable) {
                            sink.addInlineElement(
                                propArgs.value.endOffset,
                                true,
                                factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                                false
                            )
                        }
                    }
                    return true
                }
            }
        }
        return null
    }
}