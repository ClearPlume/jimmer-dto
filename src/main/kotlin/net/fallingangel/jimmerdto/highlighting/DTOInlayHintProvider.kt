package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTONegativeProp
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.element.DTOValue
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JLabel

@Suppress("UnstableApiUsage")
class DTOInlayHintProvider : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("NoSettings")

    override val name: String
        get() = "JimmerDTOPropNullabilityHint"

    override val previewText: String?
        get() = null

    override fun createConfigurable(settings: NoSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener) = JLabel("NoSettings")
    }

    override fun createSettings() = NoSettings()

    override fun isLanguageSupported(language: Language) = language == DTOLanguage

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        if (file !is DTOFile) return null

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element.project.isDefault || !element.isValid) return false

                val (property, offset) = when (element) {
                    is DTOPositiveProp -> element.property to element.name.endOffset
                    is DTONegativeProp -> element.property to element.endOffset
                    is DTOValue -> element.property to element.endOffset
                    is DTOQualifiedNamePart -> (element.target as? Resolution.Target.Property)?.property to element.endOffset
                    else -> return true
                }

                property ?: return true

                if (property.nullable) {
                    sink.addInlineElement(
                        offset,
                        true,
                        factory.roundWithBackgroundAndSmallInset(factory.text("?")),
                        false,
                    )
                }

                return true
            }
        }
    }
}
