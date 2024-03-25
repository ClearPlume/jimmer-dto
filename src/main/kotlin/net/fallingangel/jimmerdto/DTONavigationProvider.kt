package net.fallingangel.jimmerdto

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropName
import net.fallingangel.jimmerdto.psi.DTOValue
import net.fallingangel.jimmerdto.psi.mixin.DTOSingleProp
import net.fallingangel.jimmerdto.util.*

@Suppress("UnstableApiUsage")
class DTONavigationProvider : DirectNavigationProvider {
    override fun getNavigationElement(element: PsiElement): PsiElement? {
        return when (element) {
            is DTOPropName -> navigating(element)
            is DTOValue -> navigating(element)
            else -> null
        }
    }

    private fun navigating(element: DTOPropName): PsiElement? {
        val prop = element.parent
        if (prop is DTOSingleProp) {
            val propName = prop.propName.text
            return navigating(prop, propName)
        }
        return null
    }

    private fun navigating(element: DTOValue): PsiElement? {
        val prop = element.parent.parent
        if (prop is DTOPositiveProp) {
            val propName = prop.propName.text
            if (propName in arrayOf("flat", "id")) {
                return navigating(prop, element.text)
            }
        }
        return null
    }

    private fun navigating(prop: DTOSingleProp, propName: String): PsiElement? {
        val entityFile = prop.virtualFile.entityFile(prop.project) ?: throw IllegalStateException()
        return when (entityFile.language) {
            Language.Java -> {
                val clazz = prop.psiClass()
                clazz.methods().find { it.name == propName }
            }

            Language.Kotlin -> {
                val clazz = prop.ktClass()
                clazz.properties().find { it.name == propName }
            }
        }
    }
}
