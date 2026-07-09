package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.modifiedBy
import org.babyfish.jimmer.sql.Id

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
    val containingLClass: LClass<*>?
        get() {
            val parent = parent
            return if (parent is DTODtoBody) {
                parent.containingLClass
            } else {
                (parent.parent as DTOAliasGroup).containingLClass
            }
        }

    val property: LProperty<*>?
        get() = containingLClass?.findProperty(name.value)

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
        val type = property?.actualType as? LClass<*> ?: return emptyList()
        val props = if (prefix.isEmpty()) {
            type.allProperties
        } else {
            val propertyType = type.findPropertyOrNull(prefix)?.actualType as? LClass<*> ?: return emptyList()
            propertyType.allProperties
        }

        val scalars = props
            .filter { it.type is LType.ScalarType || it.type is LType.EnumType<*, *> }
            .map { it.name to it.presentableType }
        val associations = props
            .filter(LProperty<*>::isReference)
            .map { it.name to it.presentableType }
        val views = props
            .filter(LProperty<*>::isEntityAssociation)
            .mapNotNull { prop ->
                val idType = prop.targetClass!!.properties
                    .find { it.hasAnnotation(Id::class) }
                    ?.presentableType
                    ?: return@mapNotNull null
                "${prop.name}Id" to idType
            }
        val embeddable = props
            .filter(LProperty<*>::isEmbedded)
            .map { it.name to it.presentableType }

        return scalars + associations + views + embeddable
    }
}