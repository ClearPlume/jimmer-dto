package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

interface DTOMacro : DTOElement {
    val hash: PsiElement

    val name: DTOMacroName

    val args: DTOMacroArgs?

    val optional: PsiElement?

    val required: PsiElement?

    val clazz: LClass<*>
        get() {
            val propPath = propPath()
            return if (propPath.isEmpty()) {
                file.clazz
            } else {
                file.clazz.findProperty(propPath).actualType as LClass<*>
            }
        }

    /**
     * 宏可用参数
     */
    val types: List<String>
        get() = clazz.allParents.map(LClass<*>::name) + clazz.name + "this"

    /**
     * 携带的属性列表
     */
    val carriedProps: List<String>
        get() {
            val argList = args?.values?.map(PsiElement::getText) ?: types
            val containThisProp = argList.any { it in listOf("this", clazz.name) }

            return when (name.value) {
                "allScalars" -> {
                    val thisProps = if (containThisProp) {
                        clazz.properties
                                .filter { !it.isEntityAssociation }
                                .map(LProperty<*>::name)
                    } else {
                        emptyList()
                    }
                    val superProps = clazz.allParents
                            .filter { argList.isEmpty() || argList.contains(it.name) }
                            .flatMap { clazz ->
                                clazz.properties
                                        .filter { !it.isEntityAssociation }
                                        .map(LProperty<*>::name)
                            }
                    thisProps + superProps
                }

                "allReferences" -> {
                    val thisProps = if (containThisProp) {
                        clazz.properties
                                .filter { it.isReference }
                                .map(LProperty<*>::name)
                    } else {
                        emptyList()
                    }
                    val superProps = clazz.allParents
                            .filter { argList.isEmpty() || argList.contains(it.name) }
                            .flatMap { clazz ->
                                clazz.properties
                                        .filter { it.isReference }
                                        .map(LProperty<*>::name)
                            }
                    thisProps + superProps
                }

                else -> emptyList()
            }
        }
}