package net.fallingangel.jimmerdto.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.refenerce.DTOReference
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext

object DTOPsiUtil {
    @JvmStatic
    fun getName(name: DTOPropName): String {
        return name.text
    }

    @JvmStatic
    fun setName(name: DTOPropName, newName: String): DTOPropName {
        name.node.replaceChild(name.identifier!!.node, name.project.createUserPropName(newName).identifier!!.node)
        return name
    }

    @JvmStatic
    fun invoke(name: DTOPropName): Array<PsiReference> {
        return when (name.parent) {
            is DTOUserProp -> emptyArray()
            else -> arrayOf(DTOReference(name, name.firstChild.textRangeInParent))
        }
    }

    @JvmStatic
    fun unaryPlus(name: DTOPropName): PsiElement? {
        return when (val prop = name.parent) {
            is DTONegativeProp -> name.fqeClass?.element(prop.propPath())

            is DTOPositiveProp -> if (prop.propArgs == null) {
                name.fqeClass?.element(prop.propPath())
            } else {
                null
            }

            else -> null
        }
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
                .takeWhile { it.elementType !in arrayOf(DTOTypes.EXPORT, DTOTypes.PACKAGE, DTOTypes.IMPORT) }
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
        val enumName = enum.text
        val prop = enum.parent.parent.parent
        val propElement = enum.fqeClass?.element(prop.propPath())
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
        return resolveMacroThis(arg.parent.parent as DTOMacro)
    }

    fun resolveMacroThis(macro: DTOMacro): PsiNamedElement? {
        val propPath = macro.propPath()
        val dtoClass = macro.fqeClass ?: return null

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
    fun getName(value: DTOValue): String {
        return value.text
    }

    @JvmStatic
    fun setName(value: DTOValue, name: String): DTOValue {
        val oldValueNode = value.node
        oldValueNode.treeParent.replaceChild(oldValueNode, value.project.createValue(name).node)
        return value
    }

    @JvmStatic
    fun invoke(value: DTOValue): Array<PsiReference> {
        return arrayOf(DTOReference(value, TextRange(0, value.textLength)))
    }

    @JvmStatic
    fun unaryPlus(value: DTOValue): PsiElement? {
        val prop = value.parent.parent as DTOPositiveProp
        val clazz = prop.fqeClass ?: return null
        val propPath = value.propPath()
        if (propPath.isEmpty()) {
            return null
        }
        return clazz.element(propPath)
    }

    @JvmStatic
    fun getName(macro: DTOMacroName) = macro.name("allScalars")

    @JvmStatic
    fun getName(name: DTODtoName): String {
        return name.text
    }

    @JvmStatic
    fun setName(name: DTODtoName, newName: String): DTODtoName {
        val oldNameNode = name.node
        oldNameNode.treeParent.replaceChild(oldNameNode, name.project.createDTOName(newName).node)
        return name
    }

    @JvmStatic
    fun invoke(name: DTODtoName): Array<PsiReference> {
        return arrayOf(DTOReference(name, name.identifier.textRangeInParent))
    }

    @JvmStatic
    fun unaryPlus(name: DTODtoName): PsiElement? {
        val project = name.project
        val dtoFile = name.containingFile as DTOFile
        return JavaPsiFacade.getInstance(project).findClass("${dtoFile.`package`}.${name.text}", ProjectScope.getAllScope(project))
    }
}