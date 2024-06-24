package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.psi.fix.*
import net.fallingangel.jimmerdto.util.*

/**
 * 部分代码结构的高亮
 */
class DTOAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(DTOAnnotatorVisitor(holder))
    }

    private class DTOAnnotatorVisitor(private val holder: AnnotationHolder) : DTOVisitor() {
        /**
         * 为注解上色
         */
        override fun visitAnnotationConstructor(o: DTOAnnotationConstructor) {
            o.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为作为参数的注解上色
         */
        override fun visitNestAnnotation(o: DTONestAnnotation) {
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            val macroName = o.macroName!!
            if (macroName.text == "allScalars") {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                o.firstChild.error()
                macroName.error()
            }
        }

        /**
         * 为宏参数上色
         */
        override fun visitMacroArgs(o: DTOMacroArgs) {
            val thisList = o.macroThisList
            if (thisList.size > 1) {
                thisList.forEach { it.error(DTOSyntaxHighlighter.DUPLICATION) }
            }

            val macroAvailableParams = o[StructureType.MacroTypes]
            for (macroArg in o.qualifiedNameList) {
                if (macroArg.text !in macroAvailableParams) {
                    macroArg.error()
                }
                if (o.qualifiedNameList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(DTOSyntaxHighlighter.DUPLICATION)
                }
                if (thisList.isNotEmpty() && macroArg.text == macroAvailableParams.last()) {
                    macroArg.error(DTOSyntaxHighlighter.DUPLICATION)
                    thisList[0].error(DTOSyntaxHighlighter.DUPLICATION)
                }
            }
        }

        /**
         * 为as组上色
         *
         * as(original -> replacement) { ... }
         */
        override fun visitAliasGroup(o: DTOAliasGroup) {
            // 『as』
            o.firstChild.style(DTOSyntaxHighlighter.FUNCTION)

            // original
            val original = o.aliasPattern.original
            val power = original.aliasPower
            val dollar = original.aliasDollar

            if (power != null && dollar != null) {
                power.error(
                    "Power and Dollar cannot both appear in the original section of AliasGroup",
                    RemoveElement("^", power)
                )

                dollar.error(
                    "Power and Dollar cannot both appear in the original section of AliasGroup",
                    RemoveElement("$", dollar)
                )
            }

            // replacement
            val stringConstant = o.aliasPattern.replacement?.stringConstant
            stringConstant?.error(
                "Unlike the usual case, string literals are not allowed here",
                ConvertStringToReplacement(stringConstant)
            )
        }

        /**
         * 为属性上色
         */
        override fun visitPositiveProp(o: DTOPositiveProp) {
            val propName = o.propName.text
            // 当前属性为方法
            if (o.propArgs != null) {
                visitFunction(o, propName)
            }
            // 当前属性为非方法属性
            if (o.propArgs == null) {
                visitProp(o, propName)
            }
            // 当前属性为枚举映射属性
            if (o.enumBody != null) {
                visitEnumMappingProp(o, propName)
            }
        }

        private fun visitFunction(o: DTOPositiveProp, propName: String) {
            val dto = o.parentOfType<DTODto>() ?: return
            val availableFunctions = if (dto modifiedBy Modifier.Specification) {
                val functions = Function.values().map { it.expression }
                val specFunctions = SpecFunction.values().map { it.expression }
                functions + specFunctions
            } else {
                Function.values().map { it.expression }
            }

            // 方法名
            val propArgs = o.propArgs!!
            if (propName in availableFunctions) {
                o.propName.style(DTOSyntaxHighlighter.FUNCTION)
            } else {
                o.propName.error()
            }

            // 方法参数
            val propAvailableArgs = propArgs[StructureType.FunctionArgs].map { it.name }
            propArgs.valueList.forEach {
                if (it.text !in propAvailableArgs) {
                    it.error()
                }
            }
        }

        private fun visitProp(o: DTOPositiveProp, propName: String) {
            val availableProperties = if (o.haveUpper) {
                val upper = o.upper
                if (upper is DTOAliasGroup) {
                    upper[StructureType.AsProperties]
                } else {
                    upper as DTOPositiveProp
                    if (upper.propName.text == "flat") {
                        val flatArg = upper.propArgs!!.valueList[0]
                        flatArg[StructureType.FlatProperties]
                    } else {
                        o[StructureType.PropProperties]
                    }
                }
            } else {
                o[StructureType.PropProperties]
            }
            availableProperties.find { it.name == propName } ?: o.propName.error()
        }

        private fun visitEnumMappingProp(o: DTOPositiveProp, propName: String) {
            val enumBody = o.enumBody!!
            val enumInstance = enumBody.enumInstanceMappingList[0].enumInstance

            val availableEnums = enumInstance[StructureType.EnumValues]
            val currentEnumNames = enumBody.enumInstanceMappingList.map { it.enumInstance.text }

            // 是否已经完成所有枚举值的映射
            val missedMappings = availableEnums.filter { it !in currentEnumNames }
            if (missedMappings.isNotEmpty()) {
                o.error(
                    if (missedMappings.size == 1) {
                        val noMappedEnum = missedMappings.first()
                        "The mapping for `$noMappedEnum` is not defined"
                    } else {
                        val allNoMappedEnum = missedMappings.joinToString()
                        "The mappings for `$allNoMappedEnum` are not defined"
                    },
                    GenerateMissedEnumMappings(propName, missedMappings, enumBody)
                )
            }
        }

        /**
         * 为用户属性上色
         */
        override fun visitUserProp(o: DTOUserProp) {
            val propName = o.propName
            val entityProperties = o[StructureType.UserPropProperties]
            entityProperties.find { propName.text == it.name }?.let {
                propName.error("Do not allow duplicate user attribute and entity attribute names", RenameElement(propName))
            }
        }

        /**
         * 为类型定义上色
         */
        override fun visitTypeDef(o: DTOTypeDef) {
            if (o.haveParent<DTOUserProp>()) {
                visitUserPropType(o)
            }
        }

        private fun visitUserPropType(o: DTOTypeDef) {
            val dtoFile = o.parentOfType<DTOFile>() ?: return
            val imports = dtoFile[StructureType.DTOFileImports]
            val preludes = dtoFile[StructureType.DTOPreludeTypes]

            val userPropType = o.qualifiedName.text
            if (userPropType !in imports + preludes) {
                o.qualifiedName.error("Unresolved reference: $userPropType", ImportClass(o.qualifiedName))
            }
        }

        override fun visitNegativeProp(o: DTONegativeProp) {
            val properties = o[StructureType.PropNegativeProperties]
            if (properties.find { it.name == o.propName.text } != null) {
                o.style(DTOSyntaxHighlighter.NOT_USED)
            } else {
                o.firstChild.style(DTOSyntaxHighlighter.NOT_USED)
                o.propName.error()
            }
        }

        /**
         * 为注解参数名上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            o.firstChild.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.firstChild.next(DTOTypes.EQUALS).style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
        }

        /**
         * 为枚举映射上色
         */
        override fun visitEnumInstanceMapping(o: DTOEnumInstanceMapping) {
            val enumBody = o.parent<DTOEnumBody>()
            val enumInstance = o.enumInstance
            val enumMappingName = enumInstance.text
            val enumMappingValue = o.enumInstanceValue

            val availableEnums = enumInstance[StructureType.EnumValues]
            val currentEnumNames = enumBody.enumInstanceMappingList.map { it.enumInstance.text }
            val currentEnumValues = enumBody.enumInstanceMappingList.mapNotNull { it.enumInstanceValue }

            val allInt = currentEnumValues.all { it.text.matches(Regex("\\d+")) }
            val allString = currentEnumValues.all { it.text.matches(Regex("\".+\"")) }
            val valueTypeValid = allInt || allString

            // 枚举映射是否存在于对应枚举中
            if (enumMappingName in availableEnums) {
                enumInstance.style(DTOSyntaxHighlighter.ENUM_INSTANCE)
            } else {
                enumInstance.error(
                    "Illegal enum mapping `$enumMappingName`",
                    RemoveElement(enumMappingName, o)
                )
            }
            // 枚举映射是否重复定义
            if (currentEnumNames.count { it == enumMappingName } != 1) {
                enumInstance.error(
                    "Duplicated enum mapping `$enumMappingName`",
                    RemoveElement(enumMappingName, o),
                    DTOSyntaxHighlighter.DUPLICATION
                )
            }
            // 枚举映射中的值是否重复定义
            if (enumMappingValue?.let { currentEnumValues.count { it.text == enumMappingValue.text } != 1 } == true) {
                enumMappingValue.error(
                    "Illegal value of enum mapping `$enumMappingName`, its duplicated",
                    RemoveElement(enumMappingValue.text, enumMappingValue),
                    DTOSyntaxHighlighter.DUPLICATION
                )
            }
            // 枚举映射值是否同一类型
            if (!valueTypeValid) {
                enumMappingValue?.error(
                    "Illegal value of enum mapping `$enumMappingName`. Integer value and String value cannot be mixed",
                    RemoveElement(enumMappingValue.text, enumMappingValue)
                )
            }
        }

        /**
         * 为全限定类名的部分上色
         */
        override fun visitQualifiedNamePart(o: DTOQualifiedNamePart) {
            if (o.haveParent<DTOPackage>() || (!o.haveParent<DTOExport>() && !o.haveParent<DTOImport>())) {
                return
            }

            when (o.parent.parent.parent.parent) {
                is DTOExport -> o.visitPackage(DTOTypes.EXPORT_KEYWORD, Project::allEntities)
                is DTOImport -> o.visitPackage(DTOTypes.IMPORT_KEYWORD, Project::allClasses)
            }
        }

        private fun DTOQualifiedNamePart.visitPackage(statementKeyword: IElementType, classes: Project.(String) -> List<PsiClass>) {
            val exportedPackage = prevLeafs
                    .takeWhile { it.elementType != statementKeyword }
                    .filter { it.parent.elementType == DTOTypes.QUALIFIED_NAME_PART }
                    .map { it.text }
                    .toList()
                    .asReversed()
            val curPackage = exportedPackage.joinToString(".")
            val curPackageClasses = project.classes(curPackage).map { it.name!! }
            val availablePackages = project.allPackages(curPackage).map { it.name!! }

            if (text !in (curPackageClasses + availablePackages)) {
                error()
            }

            // 包不能被导入
            if (nextSibling == null && text !in curPackageClasses) {
                error()
            }
        }

        /**
         * Dto修饰符上色
         */
        override fun visitModifier(o: DTOModifier) {
            val parent = o.parent
            if (parent is DTODto) {
                val currentModifiers = parent.modifierList.toModifier()
                val modifier = o.text

                if (currentModifiers.count { it.name.lowercase() == modifier } != 1) {
                    o.error(
                        "Duplicated modifier `$modifier`",
                        RemoveElement(modifier, o),
                        DTOSyntaxHighlighter.DUPLICATION
                    )
                }
            }
        }

        private fun PsiElement.next(elementType: IElementType): PsiElement {
            return nextLeaf { it.elementType == elementType }!!
        }

        private fun PsiElement.style(style: TextAttributesKey) = annotator(style, HighlightSeverity.INFORMATION)

        private fun PsiElement.error(style: TextAttributesKey = DTOSyntaxHighlighter.ERROR) = annotator(style, HighlightSeverity.ERROR)

        private fun PsiElement.annotator(style: TextAttributesKey, severity: HighlightSeverity) {
            holder.newSilentAnnotation(severity)
                    .range(this)
                    .textAttributes(style)
                    .create()
        }

        // private fun PsiElement.warning(style: TextAttributesKey, message: String? = null, fix: BaseIntentionAction? = null) {
        //     if (message == null || fix == null) {
        //         annotator(style, HighlightSeverity.WARNING)
        //     } else {
        //         fix(style, HighlightSeverity.WARNING, ProblemHighlightType.WARNING, message, fix)
        //     }
        // }

        private fun PsiElement.error(
            message: String,
            fix: BaseIntentionAction,
            style: TextAttributesKey = DTOSyntaxHighlighter.ERROR,
            highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR
        ) {
            fix(style, HighlightSeverity.ERROR, highlightType, message, fix)
        }

        private fun PsiElement.fix(
            style: TextAttributesKey,
            severity: HighlightSeverity,
            highlightType: ProblemHighlightType,
            message: String,
            fix: BaseIntentionAction
        ) {
            holder.newAnnotation(severity, message)
                    .range(this)
                    .textAttributes(style)
                    .highlightType(highlightType)
                    .withFix(fix)
                    .create()
        }
    }
}
