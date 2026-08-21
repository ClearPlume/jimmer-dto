package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationOwner
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.modifiedBy

interface DTOPositiveProp : DTOElement, DTOAnnotationOwner {
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

    override val annotationSite: LAnnotationSite
        get() = LAnnotationSite.Prop

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
}
