package net.fallingangel.jimmerdto.formatting

import com.intellij.formatting.*
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.DTOLanguage.rule
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.psi.DTOParser.*

class Formatter : FormattingModelBuilder {
    override fun createModel(context: FormattingContext): FormattingModel {
        val styleSettings = context.codeStyleSettings

        val comments = DTOLanguage.tokenSet(LineComment, BlockComment, DocComment)
        val dtoProps = DTOLanguage.ruleSet(RULE_userProp, RULE_macro, RULE_positiveProp, RULE_aliasGroup, RULE_negativeProp)
        val braces = DTOLanguage.tokenSet(LParen, RParen, LBracket, RBracket, LessThan, GreaterThan)
        val parents = DTOLanguage.ruleSet(RULE_dtoBody, RULE_groupedImport, RULE_aliasGroupBody, RULE_enumBody)

        val spacingBuilder = SpacingBuilder(styleSettings, DTOLanguage)
                // special
                .betweenInside(token[RParen], rule[RULE_aliasGroupBody], rule[RULE_aliasGroup]).spaces(1)
                .around(braces).spaces(0)

                // common
                .around(token[Comma], 0, 1)
                .around(token[Colon], 0, 1)
                .after(token[At]).spaces(0)
                .after(token[Hash]).spaces(0)
                .around(token[Equals]).spaces(1)
                .afterInside(token[Minus], rule[RULE_negativeProp]).spaces(0)
                .around(token[Minus]).spaces(1)
                .around(token[Dot]).spaces(0)
                .around(token[Arrow]).spaces(1)
                .between(token[LBrace], token[RBrace]).spaces(0)
                .around(token[Modifier]).spaces(1)
                .around(token[Implements]).spaces(1)
                .around(token[As]).spaces(1)
                .after(comments).emptyLine(0)
                .afterInside(token[LBrace], parents).emptyLine(0)
                .beforeInside(token[RBrace], parents).emptyLine(0)

                // psi elements
                .after(rule[RULE_exportStatement]).emptyLine(1)
                .around(token[Export]).spaces(1)
                .afterInside(rule[RULE_qualifiedName], token[Export]).emptyLine(0)
                .after(token[Package]).spaces(1)
                .between(rule[RULE_importStatement], rule[RULE_importStatement]).emptyLine(0)
                .around(token[Import]).spaces(1)
                .between(rule[RULE_importStatement], rule[RULE_dto]).emptyLine(1)
                .between(rule[RULE_dto], rule[RULE_dto]).emptyLine(1)
                .betweenInside(rule[RULE_annotation], rule[RULE_dtoBody], rule[RULE_propBody]).spaces(1)
                .between(rule[RULE_annotation], rule[RULE_annotation]).spaces(1)
                .around(rule[RULE_annotationValue]).spaces(0)
                .around(rule[RULE_dtoName]).spaces(1)
                .around(rule[RULE_implements]).spaces(1)
                .before(rule[RULE_dtoBody]).spaces(1)
                .around(dtoProps).emptyLine(1)
                .after(rule[RULE_propConfig]).emptyLine(0)
                .beforeInside(DTOLanguage.tokenSet(QuestionMark, ExclamationMark, Star), rule[RULE_positiveProp]).spaces(0)
                .between(rule[RULE_propName], rule[RULE_propArg]).spaces(0)
                .around(rule[RULE_propName]).spaces(1)
                .betweenInside(rule[RULE_qualifiedName], rule[RULE_genericArguments], rule[RULE_typeRef]).spaces(0)
                .before(rule[RULE_propBody]).spaces(1)
                .after(rule[RULE_enumMapping]).emptyLine(0)

        return FormattingModelProvider.createFormattingModelForPsiFile(
            context.containingFile,
            DTOBlock(spacingBuilder, context.node, Wrap.createWrap(WrapType.NONE, false), null),
            styleSettings,
        )
    }
}