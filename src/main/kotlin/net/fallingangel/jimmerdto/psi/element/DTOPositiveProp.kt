package net.fallingangel.jimmerdto.psi.element

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.modifiedBy
import net.fallingangel.jimmerdto.util.propPath

interface DTOPositiveProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val configs: List<DTOPropConfig>

    val modifier: Modifier?

    val name: DTOPropName

    val flag: DTOPropFlag?

    val arg: DTOPropArg?

    val body: DTOPropBody?

    val alias: DTOAlias?

    val optional: PsiElement?

    val required: PsiElement?

    val recursive: PsiElement?

    val property: LProperty<*>?
        get() = if (arg == null) {
            file.clazz.findPropertyOrNull(propPath())
        } else {
            null
        }

    fun functions(): List<LookupInfo> {
        val dto = parentOfType<DTODto>() ?: return emptyList()
        val functions = listOf(
            LookupInfo("id", "id()", "function", "(<association>)", -1),
            LookupInfo("flat", "flat() {}", "function", "(<association>) { ... }", -4)
        )
        val specFunctions = if (dto modifiedBy Modifier.Specification) {
            SpecFunction.values()
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

    fun allSiblings(withSelf: Boolean = false): List<LProperty<*>> {
        val propPath = propPath()
        val proceedPath = if ((withSelf || name.value in SpecFunction.values().map { it.expression }) && propPath.isNotEmpty()) {
            propPath.dropLast(1) + propPath.last().replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "")
        } else {
            propPath.dropLast(1)
        }
        return if (proceedPath.isEmpty()) {
            file.clazz.allProperties
        } else {
            val parentClazz = file.clazz.findPropertyOrNull(proceedPath)?.actualType as? LClass<*> ?: return emptyList()
            parentClazz.allProperties
        }
    }
}