package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOMacro : DTOElement {
    val hash: PsiElement

    val name: DTOMacroName

    val args: DTOMacroArgs?

    val optional: PsiElement?

    val required: PsiElement?

    //      / dtoBody
    // macro
    //      \ aliasGroupBody -> aliasGroup
    val containingLClass: LClass<*>?
        get() {
            val parent = parent

            return if (parent is DTODtoBody) {
                parent.containingLClass
            } else {
                (parent.parent as DTOAliasGroup).containingLClass
            }
        }

    /**
     * 宏可用参数
     */
    val types: List<String>
        get() {
            val clazz = containingLClass ?: return emptyList()
            return clazz.allParents.map(LClass<*>::name) + clazz.name + "this"
        }

    val isScalar: Boolean
        get() = name.value == "allScalars"

    /**
     * 携带的属性列表
     */
    val carriedProps: List<String>
        get() {
            val clazz = containingLClass ?: return emptyList()

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