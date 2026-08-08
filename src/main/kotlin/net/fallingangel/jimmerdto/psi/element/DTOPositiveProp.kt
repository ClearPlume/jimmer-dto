package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.lsi.jimmer.*
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.modifiedBy

interface DTOPositiveProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val configs: List<DTOPropConfig>

    val modifier: Modifier?

    val name: DTOPropName

    val flag: DTOPropFlag?

    val arg: DTOPropArg?

    val body: DTOPropBody?

    val `as`: PsiElement?

    val alias: DTOAlias?

    val optional: PsiElement?

    val required: PsiElement?

    val recursive: PsiElement?

    //             / dtoBody
    // positiveProp 
    //             \ aliasGroupBody -> aliasGroup
    val containingLClass: LClass?
        get() {
            val parent = parent
            return if (parent is DTODtoBody) {
                parent.containingLClass
            } else {
                (parent.parent as DTOAliasGroup).containingLClass
            }
        }

    val property: LProperty?
        get() = containingLClass?.findProperty(name.value)

    val isOptional: Boolean
        get() = optional != null

    val isRequired: Boolean
        get() = required != null

    val isRecursive: Boolean
        get() = recursive != null

    fun functions(): List<LookupInfo> {
        val dto = parentOfType<DTODto>() ?: return emptyList()
        val functions = listOf(
            LookupInfo("id", "id()", "function", "(<association>)", -1),
            LookupInfo("flat", "flat() {}", "function", "(<association>) { ... }", -4),
            LookupInfo("fold", "fold() {}", "function", "(<name>) { ... }", -4),
        )
        val specFunctions = if (dto modifiedBy Modifier.Specification) {
            Function.entries
                .filter(Function::whetherSpec)
                .map {
                    with(it) {
                        val argPresentation = if (whetherMultiArg) {
                            "<prop, prop, prop, ...>"
                        } else {
                            "<prop>"
                        }
                        LookupInfo(
                            expression,
                            "$expression()",
                            "function",
                            "($argPresentation)",
                            -1
                        )
                    }
                }
        } else {
            emptyList()
        }
        return functions + specFunctions
    }

    fun childProps(prefix: List<String>): List<Pair<String, String>> {
        val clazz = property?.actualType?.resolvedLClass ?: return emptyList()
        val props = if (prefix.isEmpty()) {
            clazz.allProperties
        } else {
            val propertyClass = clazz.findProperty(prefix)?.actualType?.resolvedLClass ?: return emptyList()
            propertyClass.allProperties
        }

        val scalars = props
            .filter { it.type is LProperty.Type.Scalar || it.type is LProperty.Type.Enum }
            .map { it.name to it.presentableType }
        val associations = props
            .filter(LProperty::isReference)
            .map { it.name to it.presentableType }
        val views = props
            .filter(LProperty::isEntityAssociation)
            .mapNotNull { prop ->
                val idType = prop.targetClass!!.properties
                    .find { it.isId }
                    ?.presentableType
                    ?: return@mapNotNull null
                "${prop.name}Id" to idType
            }
        val embeddable = props
            .filter(LProperty::isEmbedded)
            .map { it.name to it.presentableType }

        return scalars + associations + views + embeddable
    }
}