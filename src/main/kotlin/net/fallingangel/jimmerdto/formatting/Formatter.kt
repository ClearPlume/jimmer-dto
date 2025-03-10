package net.fallingangel.jimmerdto.formatting

import com.intellij.formatting.*
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOTokenTypes
import net.fallingangel.jimmerdto.psi.DTOTypes.*

class Formatter : FormattingModelBuilder {
    override fun createModel(context: FormattingContext): FormattingModel {
        val styleSettings = context.codeStyleSettings
        val dtoProps = TokenSet.create(USER_PROP, MACRO, POSITIVE_PROP, ALIAS_GROUP, NEGATIVE_PROP)
        val braces = TokenSet.create(PAREN_L, PAREN_R, BRACKET_L, BRACKET_R, LT, GT)
        val parents = TokenSet.create(DTO_BODY, ALIAS_GROUP_BODY, ENUM_BODY)

        val spacingBuilder = SpacingBuilder(styleSettings, DTOLanguage)
                // special
                .betweenInside(PAREN_R, ALIAS_GROUP_BODY, ALIAS_GROUP).spaces(1)
                .around(braces).spaces(0)

                // common
                .around(COMMA, 0, 1)
                .after(AT).spaces(0)
                .after(HASH).spaces(0)
                .around(EQ).spaces(1)
                .afterInside(MINUS, NEGATIVE_PROP).spaces(0)
                .around(MINUS).spaces(1)
                .around(DOT).spaces(0)
                .around(ARROW).spaces(1)
                .between(BRACE_L, BRACE_R).spaces(0)
                .around(MODIFIER).spaces(1)
                .around(IMPLEMENTS).spaces(1)
                .around(AS).spaces(1)
                .after(DTOTokenTypes.comments).emptyLine(0)

                // psi elements
                .after(EXPORT_STATEMENT).emptyLine(1)
                .around(EXPORT).spaces(1)
                .afterInside(QUALIFIED_TYPE, EXPORT).emptyLine(0)
                .after(PACKAGE).spaces(1)
                .between(IMPORT_STATEMENT, IMPORT_STATEMENT).emptyLine(0)
                .around(IMPORT).spaces(1)
                .between(IMPORT_STATEMENT, DTO).emptyLine(1)
                .between(DTO, DTO).emptyLine(1)
                .betweenInside(ANNOTATION, DTO_BODY, PROP_BODY).spaces(1)
                .after(ANNOTATION).emptyLine(0)
                .around(ANNOTATION_VALUE).spaces(0)
                .after(PROP_CONFIG).emptyLine(0)
                .around(DTO_NAME).spaces(1)
                .around(INTERFACES).spaces(1)
                .afterInside(BRACE_L, parents).emptyLine(0)
                .beforeInside(BRACE_R, parents).emptyLine(0)
                .around(dtoProps).emptyLine(1)
                .beforeInside(TokenSet.create(OPTIONAL, REQUIRED, ASTERISK), POSITIVE_PROP).spaces(0)
                .between(PROP_NAME, PROP_ARGS).spaces(0)
                .around(PROP_NAME).spaces(1)
                .betweenInside(QUALIFIED_NAME, GENERIC_ARGS, TYPE_DEF).spaces(0)
                .before(DTO_BODY).spaces(1)
                .before(PROP_BODY).spaces(1)

        return FormattingModelProvider.createFormattingModelForPsiFile(
            context.containingFile,
            DTOBlock(spacingBuilder, context.node, Wrap.createWrap(WrapType.NONE, false), null),
            styleSettings,
        )
    }
}