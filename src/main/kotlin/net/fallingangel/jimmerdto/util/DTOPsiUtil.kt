package net.fallingangel.jimmerdto.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.refenerce.DTOReference
import net.fallingangel.jimmerdto.refenerce.DTOValueReference

object DTOPsiUtil {
    @JvmStatic
    fun getName(prop: DTOPositiveProp): String {
        return prop.propName.text
    }

    @JvmStatic
    fun setName(prop: DTOPositiveProp, name: String): DTOPositiveProp {
        val oldNameNode = prop.node.findChildByType(DTOTypes.PROP_NAME) ?: return prop
        prop.node.replaceChild(oldNameNode, prop.project.createUserPropName(name).node)
        return prop
    }

    @JvmStatic
    fun invoke(prop: DTOPositiveProp): Array<PsiReference> {
        val propArgs = prop.propArgs
        @Suppress("IfThenToElvis")
        return if (propArgs == null) {
            arrayOf(DTOReference(prop, prop.propName.textRangeInParent))
        } else {
            // 属性方法中的value值
            propArgs.valueList
                    .map {
                        val valueStart = it.textRangeInParent.startOffset
                        val argStart = it.parent.textRangeInParent.startOffset
                        val valueInArgStart = argStart + valueStart
                        DTOValueReference(it, prop, TextRange(valueInArgStart, valueInArgStart + it.textLength))
                    }
                    .toTypedArray()
        }
    }

    @JvmStatic
    fun unaryPlus(prop: DTOPositiveProp): PsiElement? {
        val project = prop.project
        val propName = prop.propName.text

        return JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project))?.element(propName)
    }

    @JvmStatic
    fun getName(prop: DTONegativeProp): String {
        return prop.propName.text
    }

    @JvmStatic
    fun setName(prop: DTONegativeProp, name: String): DTONegativeProp {
        val oldNameNode = prop.node.findChildByType(DTOTypes.PROP_NAME) ?: return prop
        prop.node.replaceChild(oldNameNode, prop.project.createUserPropName(name).node)
        return prop
    }

    @JvmStatic
    fun invoke(prop: DTONegativeProp): Array<PsiReference> {
        return arrayOf(DTOReference(prop, prop.propName.textRangeInParent))
    }

    @JvmStatic
    fun unaryPlus(prop: DTONegativeProp): PsiElement? {
        val project = prop.project
        val propName = prop.propName.text

        return JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project))?.element(propName)
    }

    @JvmStatic
    fun setName(value: DTOValue, name: String): DTOValue {
        val valueNode = value.node
        valueNode.treeParent.replaceChild(valueNode, value.project.createValue(name).node)
        return value
    }
}