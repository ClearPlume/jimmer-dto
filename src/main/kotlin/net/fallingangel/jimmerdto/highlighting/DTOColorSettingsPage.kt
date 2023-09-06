package net.fallingangel.jimmerdto.highlighting

import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import net.fallingangel.jimmerdto.Constant

class DTOColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors() = arrayOf(
        AttributesDescriptor("Comments//LineComment", DTOSyntaxHighlighter.LINE_COMMENT),
        AttributesDescriptor("Comments//BlockComment", DTOSyntaxHighlighter.BLOCK_COMMENT),
        AttributesDescriptor("Comments//DocComment", DTOSyntaxHighlighter.DOC_COMMENT),
        AttributesDescriptor("Keywords//Keyword", DTOSyntaxHighlighter.KEYWORD),
        AttributesDescriptor("Keywords//Modifier", DTOSyntaxHighlighter.MODIFIER),
        AttributesDescriptor("Props//Function", DTOSyntaxHighlighter.FUNCTION),
        AttributesDescriptor("Props//Macro", DTOSyntaxHighlighter.MACRO),
        AttributesDescriptor("Props//NegativeProp", DTOSyntaxHighlighter.NEGATIVE_PROP),
        AttributesDescriptor("Annotation", DTOSyntaxHighlighter.ANNOTATION),
        AttributesDescriptor("Values//Character", DTOSyntaxHighlighter.CHAR),
        AttributesDescriptor("Values//String", DTOSyntaxHighlighter.STRING),
        AttributesDescriptor("Values//Number", DTOSyntaxHighlighter.NUMBER),
        AttributesDescriptor("Bad value", HighlighterColors.BAD_CHARACTER)
    )

    override fun getAdditionalHighlightingTagToDescriptorMap() = mapOf(
        "annotation-constructor" to DTOSyntaxHighlighter.ANNOTATION,
        "function" to DTOSyntaxHighlighter.FUNCTION,
        "macro" to DTOSyntaxHighlighter.MACRO
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName() = Constant.NAME

    override fun getIcon() = Constant.ICON

    override fun getHighlighter() = DTOSyntaxHighlighter()

    override fun getDemoText(): String {
        return """
        // You are reading the ".dto" entry.
        /*
         * This is a multiline
         * comments.
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
        
        /**
         * Doc
         */
        <annotation-constructor>@a.b.S</annotation-constructor>
        Book : A, B, C, D {
            <macro>#allScalars</macro>
            sdf234
            +sdf

            @B
            -sff

            @A
            sd as fds
            sdf: a.d.f.Int
            sdfGen: a.d.f.Int<out T, in F<D, in V>>
            <function>as</function>(sdf -> fds) {
                <macro>#allScalars</macro>
            }
            <function>flat</function>(parent) <annotation-constructor>@Shallow</annotation-constructor> {
                <macro>#allScalars</macro>
            }
            <function>id</function>(asd)
            <function>id</function>(dsa) as fd
        }
        
        // Annotation
        <annotation-constructor>@Shallow</annotation-constructor>(true, fdsdf = "false", sdf = null, age = 15.3, height = 180, sdf = '0', arr = [10, 20])
        abstract Book {}
        
        <annotation-constructor>@D</annotation-constructor>(<annotation-constructor>@N</annotation-constructor>, c = <annotation-constructor>@k.l.M</annotation-constructor>(20), d = <annotation-constructor>F</annotation-constructor>(), e = <annotation-constructor>@E</annotation-constructor>, arr = {23, 30})
        abstract input Book {}
        
        input-only Book {}
    """.trimIndent()
    }
}
