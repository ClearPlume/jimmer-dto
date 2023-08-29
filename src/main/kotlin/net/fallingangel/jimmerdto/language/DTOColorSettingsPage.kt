package net.fallingangel.jimmerdto.language

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import net.fallingangel.jimmerdto.Constant

class DTOColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors() = arrayOf(
        AttributesDescriptor("Comments//LineComment", DTOSyntaxHighlighter.LINE_COMMENT),
        AttributesDescriptor("Comments//BlockComment", DTOSyntaxHighlighter.BLOCK_COMMENT),
        AttributesDescriptor("Keywords//Keyword", DTOSyntaxHighlighter.KEYWORD),
        AttributesDescriptor("Keywords//Modifier", DTOSyntaxHighlighter.MODIFIER),
        AttributesDescriptor("Values//Character", DTOSyntaxHighlighter.CHAR),
        AttributesDescriptor("Values//String", DTOSyntaxHighlighter.STRING),
        AttributesDescriptor("Values//Number", DTOSyntaxHighlighter.NUMBER),
        AttributesDescriptor("Annotation", DTOSyntaxHighlighter.ANNOTATION),
        AttributesDescriptor("Bad value", HighlighterColors.BAD_CHARACTER)
    )

    override fun getAdditionalHighlightingTagToDescriptorMap() = mapOf(
        "annotation-constructor" to DTOSyntaxHighlighter.ANNOTATION
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = Constant.NAME

    override fun getIcon() = Constant.ICON

    override fun getHighlighter() = DTOSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
        // You are reading the ".dto" entry.
        /*
         This is a multiline
         comments.
         */
        // import
        import abc.sdf
        import abc.Def as D
        import sdf.sdf.Sdf as S
        import abc.def.{
            Aaa as DA,
            Bbb,
            aks,
            B
        }
        
        // Dto
        <annotation-constructor>@a.b.S</annotation-constructor>
        Book { sdf }
        
        <annotation-constructor>@Shallow</annotation-constructor>(true, fdsdf = "false", sdf = null, age = 15.3, height = 180, sdf = '0', arr = [10, 20])
        abstract Book { sdf }
        
        <annotation-constructor>@D</annotation-constructor>(<annotation-constructor>@N</annotation-constructor>, c = <annotation-constructor>@k.l.M</annotation-constructor>(20), arr = {23, 30})
        abstract input Book { sdf }
        
        input-only Book { sdf }
    """.trimIndent()
    }
}
