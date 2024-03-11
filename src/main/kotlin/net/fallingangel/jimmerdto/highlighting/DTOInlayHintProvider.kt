package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.Label
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTONegativeProp
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

    override fun createConfigurable(settings: NoSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener) = Label("NoSettings")
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

                    if (element.parent is DTONegativeProp) {
                        return false
                    }

                    val elementProp = element.parent as? DTOPositiveProp ?: return false
                    // 不是方法才走这个逻辑
                    val propArgs = elementProp.propArgs
                    if (propArgs == null) {
                        val properties = elementProp[StructureType.PropProperties]
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
                        val properties = elementProp[StructureType.PropProperties]
                        for (arg in propArgs.valueList) {
                            val prop = properties.find { it.name == arg.text } ?: continue
                            if (prop.nullable) {
                                sink.addInlineElement(
                                    arg.endOffset,
                                    true,
                                    factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                                    false
                                )
                            }
                        }
                    }
                    return false
                }
            }
        }
        return null
    }
}