package net.fallingangel.jimmerdto.psi.element

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.grammarMismatch
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

interface DTOAnnotationSingleValue : DTOElement {
    val boolean: PsiElement?

    val character: PsiElement?

    val string: List<PsiElement>

    val integer: PsiElement?

    val float: PsiElement?

    val nestAnnotation: DTONestAnnotation?

    val qualifiedName: DTOQualifiedName?

    val classSuffix: PsiElement?

    val value: ParamValue?
        get() {
            val boolean = boolean?.text?.toBoolean()
            val character = character?.unquote()?.single()
            val string = string.takeIf(List<*>::isNotEmpty)?.joinToString("") { it.unquote() }
            val integer = integer?.text?.let { it.toIntOrNull() ?: it.toLongOrNull() }
            val float = float?.text?.toDoubleOrNull()
            val nestAnnotation = nestAnnotation
            val qualifiedName = qualifiedName
            val classSuffix = classSuffix

            return when {
                boolean != null -> ParamValue.Scalar(boolean)
                character != null -> ParamValue.Scalar(character)
                string != null -> ParamValue.Scalar(string)
                integer != null -> ParamValue.Scalar(integer)
                float != null -> ParamValue.Scalar(float)
                nestAnnotation != null -> nestAnnotation.lAnnotation?.let(ParamValue::Annotation)

                qualifiedName != null -> {
                    if (classSuffix == null) {
                        val target = qualifiedName.target as? Resolution.Target.EnumConst ?: return null
                        val (canonicalName, constantName) = process(target.enum) { qualifiedEnumConstant() } ?: return null
                        ParamValue.Enum(canonicalName, constantName)
                    } else {
                        val target = qualifiedName.target as? Resolution.Target.Type ?: return null
                        val qualifiedName = process(target.type) { classQualifiedName() } ?: return null
                        ParamValue.Clazz(qualifiedName)
                    }
                }

                else -> grammarMismatch()
            }
        }

    private fun PsiElement.unquote(): String {
        return StringUtil.unescapeStringCharacters(StringUtil.unquoteString(text))
    }
}