package net.fallingangel.jimmerdto.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.TokenType
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
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
        return JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project))?.element(prop.propPath())
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
        return JavaPsiFacade.getInstance(project).findClass(prop.fqe, ProjectScope.getAllScope(project))?.element(prop.propPath())
    }

    @JvmStatic
    fun getName(qualifiedPart: DTOQualifiedNamePart): String {
        return qualifiedPart.text
    }

    @JvmStatic
    fun setName(qualifiedPart: DTOQualifiedNamePart, name: String): DTOQualifiedNamePart {
        val oldPartNode = qualifiedPart.node
        oldPartNode.treeParent.replaceChild(oldPartNode, qualifiedPart.project.createQualifiedNamePart(name).node)
        return qualifiedPart
    }

    @JvmStatic
    fun invoke(qualifiedPart: DTOQualifiedNamePart): Array<PsiReference> {
        return arrayOf(DTOReference(qualifiedPart, qualifiedPart.identifier.textRangeInParent))
    }

    @JvmStatic
    fun unaryPlus(qualifiedPart: DTOQualifiedNamePart): PsiElement? {
        val part = qualifiedPart.text
        val `package` = qualifiedPart.prevLeafs
                .takeWhile { it.elementType !in arrayOf(DTOTypes.EXPORT_KEYWORD, DTOTypes.PACKAGE_KEYWORD, DTOTypes.IMPORT_KEYWORD) }
                .filter { it.elementType != TokenType.WHITE_SPACE }
                .map(PsiElement::getText)
                .toList()
                .asReversed()
                .joinToString("")
        val qualified = if (`package`.isEmpty()) {
            part
        } else {
            "$`package`$part"
        }
        val psiFacade = JavaPsiFacade.getInstance(qualifiedPart.project)
        return psiFacade.findClass(qualified, ProjectScope.getAllScope(qualifiedPart.project)) ?: psiFacade.findPackage(qualified)
    }

    @JvmStatic
    fun setName(value: DTOValue, name: String): DTOValue {
        val oldValueNode = value.node
        oldValueNode.treeParent.replaceChild(oldValueNode, value.project.createValue(name).node)
        return value
    }
}