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
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ValueType

val ParamType.placeholder: String
    get() = when (this) {
        is Scalar -> when (kind) {
            ScalarKind.STRING -> "\"\""
            ScalarKind.INT, ScalarKind.LONG, ScalarKind.BYTE, ScalarKind.SHORT -> "0"
            ScalarKind.BOOLEAN -> "false"
            ScalarKind.CHAR -> "' '"
            ScalarKind.FLOAT, ScalarKind.DOUBLE -> "0.0"
        }

        is ParamType.Enum -> constants.firstOrNull()?.let { "$canonicalName.${it.key}" } ?: canonicalName
        is Clazz -> "Any::class"
        is ParamType.Annotation -> "@$canonicalName()"
        is ParamType.Array -> "{}"
    }

val ParamType.templateExpression: Expression
    get() = when (this) {
        is Scalar -> when (kind) {
            ScalarKind.BYTE, ScalarKind.SHORT, ScalarKind.INT, ScalarKind.LONG -> ConstantNode("0")
            ScalarKind.FLOAT, ScalarKind.DOUBLE -> ConstantNode("0.0")
            ScalarKind.CHAR, ScalarKind.STRING -> ConstantNode("")
            ScalarKind.BOOLEAN -> ConstantNode("false").withLookupStrings("true", "false")
        }

        is ParamType.Enum -> {
            val value = if (constants.isEmpty()) {
                canonicalName
            } else {
                "${canonicalName}.${constants.keys.first()}"
            }
            val lookupElements = constants
                .map { (name, element) ->
                    LookupElementBuilder.create(element, "${canonicalName}.$name")
                        .withIcon(element.getIcon(0))
                }
            ConstantNode(value).withLookupItems(lookupElements)
        }

        else -> ConstantNode(placeholder)
    }

val ValueType.typeName: String
    get() = when (this) {
        is ValueType.Scalar -> when (value) {
            is String -> "String"; is Boolean -> "Boolean"; is Char -> "Char"
            is Int -> "Int"; is Long -> "Long"; is Double -> "Double"
            else -> value::class.simpleName ?: "?"
        }

        is ValueType.Enum -> canonicalName
        is ValueType.Clazz -> presentation
        is ValueType.Annotation -> annotation.canonicalName
        is ValueType.Array -> elements.filterNotNull().firstOrNull()?.typeName?.plus("[]") ?: "Array"
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
