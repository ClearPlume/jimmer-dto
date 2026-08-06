package net.fallingangel.jimmerdto.formatting

import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import net.fallingangel.jimmerdto.DTOLanguage

class DTOLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = DTOLanguage

    override fun getCodeSample(settingsType: SettingsType): String {
        return """
            export com.example.Book

            BookView {
                #allScalars
                store {
                    name
                }
            }
        """.trimIndent()
    }

    override fun customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions) {
        commonSettings.apply {
            LINE_COMMENT_AT_FIRST_COLUMN = false
            LINE_COMMENT_ADD_SPACE = true
            BLOCK_COMMENT_AT_FIRST_COLUMN = false
        }
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {}
}