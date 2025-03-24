package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.Label
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOPropName
import net.fallingangel.jimmerdto.psi.element.DTOValue
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath
import org.jetbrains.kotlin.psi.psiUtil.endOffset

@Suppress("UnstableApiUsage")
class DTOInlayHintProvider : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("NoSettings")

    override val name: String
        get() = "JimmerDTOPropNullabilityHint"

    override val previewText: String?
        get() = null

    override fun createConfigurable(settings: NoSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener) = Label("NoSettings")
    }

    override fun createSettings() = NoSettings()

    override fun isLanguageSupported(language: Language) = language == DTOLanguage

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        if (file is DTOFile) {
            return object : FactoryInlayHintsCollector(editor) {
                override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                    if (element.project.isDefault || !element.isValid) {
                        return false
                    }

                    when (element) {
                        is DTOPropName -> {
                            val prop = element.parent
                            if (prop is DTOPositiveProp) {
                                prop.collect(sink, factory)
                            }
                        }

                        is DTOValue -> {
                            element.collect(sink, factory)
                        }
                    }

                    return true
                }
            }
        }
        return null
    }

    private fun DTOPositiveProp.collect(sink: InlayHintsSink, factory: PresentationFactory) {
        if (arg == null) {
            val property = file.clazz.findPropertyOrNull(propPath()) ?: return
            if (property.nullable) {
                sink.addInlineElement(
                    name.endOffset,
                    true,
                    factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                    false,
                )
            }
        }
    }

    private fun DTOValue.collect(sink: InlayHintsSink, factory: PresentationFactory) {
        val property = file.clazz.findPropertyOrNull(parent.parent.propPath() + text) ?: return
        if (property.nullable) {
            sink.addInlineElement(
                endOffset,
                true,
                factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                false,
            )
        }
    }
}