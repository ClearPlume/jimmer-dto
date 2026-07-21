package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.lsi.jimmer.*
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import org.babyfish.jimmer.sql.ExcludeFromAllScalars
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.ManyToManyView

interface DTOMacro : DTOElement {
    val hash: PsiElement

    val name: DTOMacroName

    val args: DTOMacroArgs?

    val optional: PsiElement?

    val required: PsiElement?

    //      / dtoBody
    // macro - aliasGroupBody -> aliasGroup
    //      \ polymorphic
    val containingLClass: LClass?
        get() {
            return when (val parent = parent) {
                is DTODtoBody -> parent.containingLClass
                is DTOPolymorphic -> null
                else -> (parent.parent as DTOAliasGroup).containingLClass
            }
        }

    /**
     * 宏可用参数
     */
    val types: List<String>
        get() {
            val clazz = containingLClass ?: return emptyList()
            return clazz.allParents.map(LClass::name) + clazz.name + "this"
        }

    /**
     * 携带的属性列表
     */
    val carriedProps: List<LProperty>
        get() {
            val clazz = containingLClass ?: return emptyList()
            val isScalar = name.value == "allScalars"
            val argList = args?.values?.map(PsiElement::getText) ?: types

            val thisPropList = if (argList.any { it in listOf("this", clazz.name) }) {
                clazz.properties
                    .filter(macroPropertyFilter(isScalar))
            } else {
                emptyList()
            }
            val superPropList = clazz.allParents
                .filter { argList.isEmpty() || argList.contains(it.name) }
                .flatMap { clazz ->
                    clazz.properties
                        .filter(macroPropertyFilter(isScalar))
                }

            return thisPropList + superPropList
        }

    private fun macroPropertyFilter(isScalar: Boolean): (LProperty) -> Boolean {
        return {
            if (isScalar) {
                !it.isEntityAssociation &&
                        !it.isFormula &&
                        !it.isTransient &&
                        !it.isList &&
                        !it.hasAnnotation(IdView::class) &&
                        !it.hasAnnotation(ManyToManyView::class) &&
                        !it.hasAnnotation(LogicalDeleted::class) &&
                        !it.hasAnnotation(ExcludeFromAllScalars::class)
            } else {
                it.isReference
            }
        }
    }
}