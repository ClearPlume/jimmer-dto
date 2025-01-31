package net.fallingangel.jimmerdto.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.refenerce.DTOReference
import net.fallingangel.jimmerdto.refenerce.DTOValueReference
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext

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
    fun getName(enum: DTOEnumInstance): String {
        return enum.text
    }

    @JvmStatic
    fun setName(enum: DTOEnumInstance, name: String): DTOEnumInstance {
        val oldEnumNode = enum.node
        oldEnumNode.treeParent.replaceChild(oldEnumNode, enum.project.createEnumMappingInstance(name).node)
        return enum
    }

    @JvmStatic
    fun invoke(enum: DTOEnumInstance): Array<PsiReference> {
        return arrayOf(DTOReference(enum, enum.identifier.textRangeInParent))
    }

    @JvmStatic
    fun unaryPlus(enum: DTOEnumInstance): PsiElement? {
        val project = enum.project
        val enumName = enum.text
        val prop = enum.parent.parent.parent
        val propElement = JavaPsiFacade.getInstance(project).findClass(enum.fqe, ProjectScope.getAllScope(project))?.element(prop.propPath())
        propElement ?: return null

        return if (propElement.language == KotlinLanguage.INSTANCE) {
            propElement as KtProperty
            val propClass = propElement.analyze()[BindingContext.TYPE, propElement.typeReference]?.clazz() ?: return null
            propClass.declarations.find { it.name == enumName }
        } else {
            propElement as PsiMethod
            val propClass = propElement.returnType?.clazz() ?: return null
            propClass.fields.find { it.name == enumName }
        }
    }

    @JvmStatic
    fun getName(arg: DTOMacroArg): String {
        return arg.text
    }

    @JvmStatic
    fun setName(arg: DTOMacroArg, name: String): DTOMacroArg {
        if (arg.text == "this") {
            return arg
        }
        val oldArgNode = arg.node
        oldArgNode.treeParent.replaceChild(oldArgNode, arg.project.createMacroArg(name).node)
        return arg
    }

    @JvmStatic
    fun invoke(arg: DTOMacroArg): Array<PsiReference> {
        return arrayOf(DTOReference(arg, arg.identifier.textRangeInParent))
    }

    @JvmStatic
    fun unaryPlus(arg: DTOMacroArg): PsiElement? {
        val `this` = resolveMacroThis(arg) ?: return null
        `this` as PsiNamedElement
        return if (arg.text !in listOf("this", `this`.name)) {
            val supers: List<PsiNamedElement> = if (`this`.language == KotlinLanguage.INSTANCE) {
                val ktClass = PsiTreeUtil.findChildOfType(`this`.virtualFile.toPsiFile(arg.project), KtClass::class.java) ?: return null
                ktClass.supers()
            } else {
                `this` as PsiClass
                `this`.supers()
            }
            supers.find { it.name == arg.identifier.text }
        } else {
            `this`
        }
    }

    private fun resolveMacroThis(arg: DTOMacroArg): PsiElement? {
        val project = arg.project
        val propPath = arg.parent.parent.propPath()
        val dtoClass = JavaPsiFacade.getInstance(project).findClass(arg.fqe, ProjectScope.getAllScope(project)) ?: return null

        return if (propPath.isEmpty()) {
            dtoClass
        } else {
            val prop = dtoClass.element(propPath) ?: return null
            if (prop.language == KotlinLanguage.INSTANCE) {
                prop as KtProperty
                prop.analyze()[BindingContext.TYPE, prop.typeReference]?.clazz()
            } else {
                prop as PsiMethod
                prop.returnType?.clazz()
            }
        }
    }

    @JvmStatic
    fun setName(value: DTOValue, name: String): DTOValue {
        val oldValueNode = value.node
        oldValueNode.treeParent.replaceChild(oldValueNode, value.project.createValue(name).node)
        return value
    }
}