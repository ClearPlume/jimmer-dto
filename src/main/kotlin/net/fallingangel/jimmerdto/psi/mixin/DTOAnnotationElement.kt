package net.fallingangel.jimmerdto.psi.mixin

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.grammarMismatch
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.parent

interface DTOAnnotationElement : DTOElement {
    val qualifiedName: DTOQualifiedName

    val value: DTOAnnotationValue?

    val params: List<DTOAnnotationParameter>

    val host: DTOAnnotationHost
        get() = parent as? DTOAnnotationHost ?: grammarMismatch()

    val values: Map<String, LAnnotation.Param.Value?>
        get() = buildMap {
            value?.let { put("value", it.value) }
            putAll(params.map { it.name.text to it.value?.value })
        }

    val lAnnotation: LAnnotation?
        get() {
            val target = qualifiedName.target as? Resolution.Target.Type ?: return null
            val (className, params) = process(target.source) { className() to lAnnotationParams(values) } ?: return null
            params ?: return null

            return LAnnotation(
                className,
                params,
                target.source,
            )
        }

    fun paramAt(position: PsiElement): LAnnotation.Param? {
        val name = position.parent<DTOAnnotationParameter>()?.name?.text ?: "value"
        return lAnnotation?.params?.find { it.name == name }
    }
}
