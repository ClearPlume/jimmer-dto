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
import com.intellij.psi.util.*
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
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
         * 为属性配置上色
         */
        override fun visitPropConfig(o: DTOPropConfig) {
            o.propConfigName.style(DTOSyntaxHighlighter.PROP_CONFIG)

            val propConfigNames = PropConfigName.values().map(PropConfigName::text)
            if (o.propConfigName.text.substring(1) !in propConfigNames) {
                val availableNames = propConfigNames.joinToString { "'$it'" }
                o.propConfigName.error("Incorrect prop-config name, available names are: $availableNames")
            }
        }

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
            if (o.firstChild.elementType == DTOTypes.AT) {
                o.firstChild.style(DTOSyntaxHighlighter.ANNOTATION)
            }
            o.annotationName.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            val macroName = o.macroName ?: return
            if (macroName.text == "allScalars") {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                macroName.error(
                    "Macro name should be \"allScalars\"",
                    ReplaceElement(macroName, o.project.createMacro().macroName!!),
                )
            }
        }

        /**
         * 为宏参数上色
         */
        override fun visitMacroArgs(o: DTOMacroArgs) {
            val argList = o.macroArgList
            if (argList.isEmpty()) {
                o.error("Macro arg list cannot be empty", InsertMacroArg(o.parent as DTOMacro))
                return
            }

            // 不允许出现超过一个<this>
            val thisList = argList.filter { it.text == "this" }
            thisList.forEach {
                it.style(DTOSyntaxHighlighter.KEYWORD)
            }
            if (thisList.size > 1) {
                thisList
                        .forEach {
                            it.error(
                                "Only one `this` is allowed",
                                RemoveElement("this", it),
                                DTOSyntaxHighlighter.DUPLICATION,
                            )
                        }
            }

            // 宏可用参数，<this>一定是最后一个
            val macroAvailableParams = (o.parent as DTOMacro)[StructureType.MacroTypes]
            for (macroArg in o.macroArgList) {
                // 当前元素不在宏可用参数中，即为非法
                if (macroArg.text !in macroAvailableParams) {
                    macroArg.error(
                        "Available parameters: [${macroAvailableParams.joinToString(", ")}]",
                        RemoveElement(macroArg.text, macroArg)
                    )
                }
                // 当前元素在参数列表中出现过一次以上，即为重复
                if (o.macroArgList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(
                        "Each parameter is only allowed to appear once",
                        RemoveElement(macroArg.text, macroArg),
                        DTOSyntaxHighlighter.DUPLICATION
                    )
                }
                // 当前实体的简单类名和this同时出现
                // 当前实体的简单类名
                val thisName = DTOPsiUtil.resolveMacroThis(o.parent as DTOMacro)?.name ?: return

                // 等价于this的宏参数
                val sameThisArg = o.macroArgList.find { it.text == thisName }
                if (macroArg.text == "this" && sameThisArg != null) {
                    sameThisArg.error(
                        "Here `$thisName` is equivalent to `this`",
                        RemoveElement(sameThisArg.text, sameThisArg),
                        DTOSyntaxHighlighter.DUPLICATION
                    )
                    macroArg.error(
                        "Here `this` is equivalent to `$thisName`",
                        RemoveElement("this", macroArg),
                        DTOSyntaxHighlighter.DUPLICATION
                    )
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

            // alias-pattern
            val aliasPattern = o.aliasPattern
            if (aliasPattern == null) {
                var foundParenR = false
                o.firstChild.nextLeafs
                        .dropWhile { it.elementType != DTOTypes.PAREN_L }
                        .takeWhile {
                            if (foundParenR) {
                                false
                            } else {
                                if (it.elementType == DTOTypes.PAREN_R) {
                                    foundParenR = true
                                }
                                true
                            }
                        }
                        .forEach { it.error("alias-pattern in alias-group cannot be empty") }
                return
            }

            // original
            val original = aliasPattern.original
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
            val stringConstant = aliasPattern.replacement?.string
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
            val propArgs = o.propArgs ?: return
            if (propArgs.valueList.isEmpty()) {
                propArgs.error("Function arg list cannot be empty")
                return
            }
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
                        val flatArg = upper.propArgs?.valueList?.get(0) ?: return
                        flatArg[StructureType.FlatProperties]
                    } else {
                        o[StructureType.PropProperties]
                    }
                }
            } else {
                o[StructureType.PropProperties]
            }
            val prop = availableProperties.find { it.name == propName } ?: let {
                o.propName.error("Prop `$propName` does not exist in the entity")
                return
            }
            if (prop.whetherAssociated && o.propBody == null && o.lastChild.elementType != DTOTypes.ASTERISK) {
                o.propName.error("Prop `$propName` must have child body")
            }
        }

        private fun visitEnumMappingProp(o: DTOPositiveProp, propName: String) {
            val enumBody = o.enumBody ?: return
            if (enumBody.enumInstanceMappingList.isEmpty()) {
                return
            }
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
                propName.error(
                    "It is prohibited for user-prop and entity prop to have the same name",
                    RenameElement(propName, Project::createUserPropName),
                )
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
            val userPropType = o.qualifiedName.text

            val imports = dtoFile[StructureType.DTOFileImports]
            val preludes = dtoFile[StructureType.DTOPreludeTypes]
            val dtos = dtoFile[StructureType.DTOFileDtos]

            if (userPropType in dtos) {
                o.qualifiedName.error(
                    "It is not allowed to use a DTO, as generated by the Jimmer DTO language, as its type when defining user-prop",
                    RemoveElement(userPropType, o.qualifiedName)
                )
                return
            }

            if (userPropType !in imports + preludes) {
                o.qualifiedName.error("Unresolved reference: $userPropType", ImportClass(o.qualifiedName))
            }
        }

        /**
         * 为负属性上色
         */
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
            o.firstChild.next(DTOTypes.EQ).style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
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
            if (o.haveParent<DTOPackageStatement>() || (!o.haveParent<DTOExportStatement>() && !o.haveParent<DTOImportStatement>())) {
                return
            }

            when (o.parent.parent.parent) {
                is DTOExportStatement -> o.visitPackage(DTOTypes.EXPORT, Project::allEntities)
                is DTOImportStatement -> o.visitPackage(DTOTypes.IMPORT, Project::allClasses)
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
                error(
                    "Unresolved reference: $text",
                    RenameElement(this, Project::createQualifiedNamePart),
                )
                return
            }

            // 包不能被导入
            if (nextSibling == null && text !in curPackageClasses) {
                val packageAction = if (statementKeyword == DTOTypes.EXPORT) {
                    "exported"
                } else {
                    "imported"
                }
                error("Packages cannot be $packageAction")
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
            fix: BaseIntentionAction? = null,
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
            fix: BaseIntentionAction? = null
        ) {
            val annotationBuilder = holder.newAnnotation(severity, message)
            annotationBuilder
                    .range(this)
                    .textAttributes(style)
                    .highlightType(highlightType)
            fix?.let { annotationBuilder.withFix(fix) }
            annotationBuilder.create()
        }
    }
}
