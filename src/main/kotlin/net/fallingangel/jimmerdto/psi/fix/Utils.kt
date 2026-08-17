package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.modcommand.ModCommandService
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.rd.util.firstOrNull
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type.Clazz
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type.Scalar
import net.fallingangel.jimmerdto.psi.DTOLexer
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type.Scalar.Kind as ScalarKind

val ParamType.placeholder: String
    get() = when (this) {
        is Scalar -> when (kind) {
            ScalarKind.String -> "\"\""
            ScalarKind.Int, ScalarKind.Long, ScalarKind.Byte, ScalarKind.Short -> "0"
            ScalarKind.Boolean -> "false"
            ScalarKind.Char -> "' '"
            ScalarKind.Float, ScalarKind.Double -> "0.0"
        }

        is ParamType.Enum -> constants.firstOrNull()?.let { "$name.${it.key}" } ?: name
        is Clazz -> "Any::class"
        is ParamType.Annotation -> "@$name()"
        is ParamType.Array -> "{}"
    }

val ParamType.templateExpression: Expression
    get() = when (this) {
        is Scalar -> when (kind) {
            ScalarKind.Byte, ScalarKind.Short, ScalarKind.Int, ScalarKind.Long -> ConstantNode("0")
            ScalarKind.Float, ScalarKind.Double -> ConstantNode("0.0")
            ScalarKind.Char, ScalarKind.String -> ConstantNode("")
            ScalarKind.Boolean -> ConstantNode("false").withLookupStrings("true", "false")
        }

        is ParamType.Enum -> {
            val value = if (constants.isEmpty()) {
                name
            } else {
                "${name}.${constants.keys.first()}"
            }
            val lookupElements = constants
                .map { (name, element) ->
                    LookupElementBuilder.create(element, "${this.name}.$name")
                        .withIcon(element.getIcon(0))
                }
            ConstantNode(value).withLookupItems(lookupElements)
        }

        else -> ConstantNode(placeholder)
    }

@Suppress("UnstableApiUsage")
fun <P : PsiElement> PsiUpdateModCommandAction<P>.asQuickFix(): LocalQuickFix {
    return ModCommandService.getInstance().wrapToQuickFix(this)
}

fun PsiElement.deleteWithAdjacentToken(relatedTokenType: Int = DTOLexer.Comma) {
    val relatedElementType = DTOLanguage.token[relatedTokenType]
    val related = getNextSiblingIgnoringWhitespace(false)?.takeIf { it.elementType == relatedElementType }
        ?: getPrevSiblingIgnoringWhitespace(false)?.takeIf { it.elementType == relatedElementType }

    related?.delete()
    delete()
}
