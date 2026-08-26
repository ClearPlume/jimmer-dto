package net.fallingangel.jimmerdto.highlighting.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBLabel
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerOptions
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.util.notModifiedBy
import net.fallingangel.jimmerdto.util.parent
import org.jetbrains.kotlin.idea.base.util.module

@Suppress("UnstableApiUsage")
class DTOInputStrategyInlayHintProvider : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("jimmer.dto.inputStrategy")

    override val name: String
        get() = "Input null strategy"

    override val previewText: String?
        get() = null

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener) = JBLabel()
        }
    }

    override fun createSettings() = NoSettings()

    override fun isLanguageSupported(language: Language) = language == DTOLanguage

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        if (file !is DTOFile) return null

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element.project.isDefault || !element.isValid) return false
                val module = element.module ?: return false

                if (element !is DTOPositiveProp) return true
                if (element.modifierElement != null) return true
                if (!element.inputStrategyApplicable) return true

                val dto = element.parent<DTODto>() ?: return true
                if (dto notModifiedBy Modifier.Input) return true

                val modifier = dto.modifiers.firstOrNull { it.isInputStrategy } ?: JimmerOptions.of(module).defaultNullableInputModifier

                sink.addInlineElement(
                    element.textOffset,
                    true,
                    factory.roundWithBackgroundAndSmallInset(factory.text(modifier.name.lowercase())),
                    false,
                )

                return true
            }
        }
    }
}
