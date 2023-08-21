package net.fallingangel.jimmerdto.language

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import net.fallingangel.jimmerdto.Constant

class DTOColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = Constant.NAME

    override fun getIcon() = Constant.ICON

    override fun getHighlighter() = DTOSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
        # You are reading the ".properties" entry.
        ! The exclamation mark can also mark text as comments.
        website = https://en.wikipedia.org/
        language = English
        # The backslash below tells the application to continue reading
        # the value onto the next line.
        message = Welcome to \
                  Wikipedia!
        # Add spaces to the key
        key\ with\ spaces = This is the value that could be looked up with the key "key with spaces".
        # Unicode
        tab : \u0009
    """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap() = null

    @Suppress("CompanionObjectInExtension")
    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Separator", DTOSyntaxHighlighter.SEPARATOR),
            AttributesDescriptor("Key", DTOSyntaxHighlighter.KEY),
            AttributesDescriptor("Value", DTOSyntaxHighlighter.VALUE),
            AttributesDescriptor("Comment", DTOSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Bad value", DTOSyntaxHighlighter.BAD_CHARACTER)
        )
    }
}
