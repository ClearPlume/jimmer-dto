package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
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
         * 为全限定类名的部分上色
         */
        override fun visitQualifiedNamePart(o: DTOQualifiedNamePart) {
            val `package` = o.parent.sibling<PsiElement>(false) {
                it.elementType == DTOLanguage.token[DTOParser.Package]
            }

            if (`package` != null) {
                return
            }

            val resolved = o.resolve()
            if (resolved == null && !o.text.endsWith("Id")) {
                o.error("Unresolved reference: ${o.part}")
            } else if ((resolved as? PsiClass)?.isAnnotationType == true) {
                o.style(DTOSyntaxHighlighter.ANNOTATION)
            } else if (resolved is PsiPackage && o.nextSibling == null && o.parent.parent is DTOAnnotation) {
                o.error("Not annotation: ${o.part}")
            } else if (resolved is PsiPackage && o.nextSibling == null && o.parent.nextSibling == null && o.parent.parent !is DTOAnnotation) {
                val packageAction = when (o.parent.parent) {
                    is DTOExportStatement -> "exported"
                    is DTOImportStatement -> "imported"
                    else -> throw IllegalStateException("There shouldn't be a third type here")
                }
                o.error("Packages cannot be $packageAction")
            }
        }

        override fun visitImportedType(o: DTOImportedType) {
            val project = o.project
            val import = o.parent.parent<DTOImportStatement>()
            val clazz = JavaPsiFacade.getInstance(project)
                    .findPackage(import.qualifiedName.value)
                    ?.classes
                    ?.find { it.name == o.type.value }
            val type = o.type.value
            if (clazz == null) {
                o.type.error("Unresolved reference: $type")
            } else {
                if (clazz.isAnnotationType) {
                    o.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }
        }

        /**
         * Dto上色
         */
        override fun visitDto(o: DTODto) {
            // 修饰符上色
            val currentModifiers = o.modifierElements
            currentModifiers.forEach { modifier ->
                if (currentModifiers.count { it.text == modifier.text } != 1) {
                    modifier.error(
                        "Duplicated modifier `${modifier.text}`",
                        RemoveElement(modifier.text, modifier),
                        DTOSyntaxHighlighter.DUPLICATION,
                    )
                }
            }
        }

        /**
         * 为注解上色
         */
        override fun visitAnnotation(o: DTOAnnotation) {
            o.at.style(DTOSyntaxHighlighter.ANNOTATION)
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为作为参数的注解上色
         */
        override fun visitNestAnnotation(o: DTONestAnnotation) {
            o.at?.style(DTOSyntaxHighlighter.ANNOTATION)
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)
        }

        /**
         * 为注解参数名上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            o.name.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.eq.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
        }

        /**
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            val macroName = o.name
            if (macroName.value == "allScalars") {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                macroName.error(
                    "Macro name should be \"allScalars\"",
                    ReplaceElement(macroName, o.project.createMacroName()),
                )
            }
        }

        /**
         * 为宏参数上色
         */
        override fun visitMacroArgs(o: DTOMacroArgs) {
            val macro = o.parent as? DTOMacro ?: return
            val argList = o.values
            if (argList.isEmpty()) {
                o.error("Macro arg list cannot be empty", InsertMacroArg(macro))
                return
            }

            // 不允许出现超过一个<this>
            val thisList = argList.filter { it.text == "this" }
            thisList.forEach { it.style(DTOSyntaxHighlighter.KEYWORD) }
            if (thisList.size > 1) {
                thisList.forEach {
                    it.error(
                        "Only one `this` is allowed",
                        RemoveElement("this", it),
                        DTOSyntaxHighlighter.DUPLICATION,
                    )
                }
            }

            // 宏可用参数，<this>一定是最后一个
            val macroAvailableParams = macro[StructureType.MacroTypes]
            for (macroArg in argList) {
                // 当前元素不在宏可用参数中，即为非法
                if (macroArg.text !in macroAvailableParams) {
                    macroArg.error(
                        "Available parameters: [${macroAvailableParams.joinToString(", ")}]",
                        RemoveElement(macroArg.text, macroArg)
                    )
                }
                // 当前元素在参数列表中出现过一次以上，即为重复
                if (argList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(
                        "Each parameter is only allowed to appear once",
                        RemoveElement(macroArg.text, macroArg),
                        DTOSyntaxHighlighter.DUPLICATION
                    )
                }

                // 当前实体的简单类名和this同时出现
                // 当前实体的简单类名
                val propPath = macro.propPath()
                val thisName = o.file.clazz.walk(propPath).name

                // 等价于this的宏参数
                val sameThisArg = argList.find { it.text == thisName }
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
         * 为负属性上色
         */
        override fun visitNegativeProp(o: DTONegativeProp) {
            val properties = o[StructureType.PropNegativeProperties]
            if (properties.any { it.name == o.name?.value }) {
                o.style(DTOSyntaxHighlighter.NEGATIVE_PROP)
            } else {
                o.name?.error()
            }
        }

        /**
         * 为as组上色
         */
        override fun visitAliasGroup(o: DTOAliasGroup) {
            o.`as`.style(DTOSyntaxHighlighter.FUNCTION)

            // alias-pattern
            val power = o.power
            val dollar = o.dollar

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
        }

        /**
         * 为用户属性上色
         */
        override fun visitUserProp(o: DTOUserProp) {
            val propName = o.name
            val entityProperties = o[StructureType.UserPropProperties]
            entityProperties.find { propName.text == it.name }?.let {
                propName.error(
                    "It is prohibited for user-prop and entity prop to have the same name",
                    RenameElement(propName, Project::createPropName),
                )
            }
        }

        /**
         * 为类型定义上色
         */
        override fun visitTypeDef(o: DTOTypeRef) {
            if (o.haveParent<DTOUserProp>()) {
                visitUserPropType(o)
            }
        }

        private fun visitUserPropType(o: DTOTypeRef) {
            if (o.type.parts.size > 1) {
                return
            }

            val dtoFile = o.file
            val type = o.type.value

            if (type in dtoFile.dtos) {
                o.type.error(
                    "It is not allowed to use a DTO, as generated by the Jimmer DTO language, as its type when defining user-prop",
                    RenameElement(o.type, Project::createUserPropType),
                )
                return
            }

            if (type !in dtoFile.imported.keys + dtoFile.importedAlias.keys + DTOLanguage.preludes) {
                o.type.error(
                    "Unresolved reference: $type",
                    ImportClass(o.type),
                )
            }
        }

        /**
         * 为属性上色
         */
        override fun visitPositiveProp(o: DTOPositiveProp) {
            val propName = o.name.value
            // 当前属性为方法
            if (o.arg != null) {
                visitFunction(o, propName)
            }
            // 当前属性为非方法属性
            if (o.arg == null) {
                visitProp(o, propName)
            }
        }

        private fun visitFunction(o: DTOPositiveProp, propName: String) {
            val dto = o.parentOfType<DTODto>() ?: return
            val availableFunctions = if (dto modifiedBy Modifier.Specification) {
                val functions = Function.values().map(Function::expression)
                val specFunctions = SpecFunction.values().map(SpecFunction::expression)
                functions + specFunctions
            } else {
                Function.values().map { it.expression }
            }

            // 方法名
            val arg = o.arg ?: return
            if (arg.values.isEmpty()) {
                arg.error("Function arg list cannot be empty")
                return
            }
            if (propName in availableFunctions) {
                o.name.style(DTOSyntaxHighlighter.FUNCTION)
            } else {
                o.name.error()
            }

            // 方法参数
            if (propName in SpecFunction.values().map { it.expression } && dto notModifiedBy Modifier.Specification) {
                o.error("Cannot call the function `$propName` because the current dto type is not specification")
            }

            arg.values.forEach {
                if (it.resolve() == null) {
                    it.error("`${it.text}` does not exist")
                }
            }

            val multiArgFunctions = SpecFunction.values().filter(SpecFunction::whetherMultiArg).map(SpecFunction::expression)
            if (arg.values.size > 1 && propName !in multiArgFunctions) {
                arg.values
                        .drop(1)
                        .forEach {
                            it.error(
                                "`$propName` accepts only one prop",
                                RemoveElement(it.text, it),
                            )
                        }
            }

            if (propName in multiArgFunctions) {
                if (arg.values.size > 1 && o.alias == null) {
                    o.error("An alias must be specified because `$propName` has multiple arguments")
                }
            }

            if (propName == "id") {
                val value = arg.values[0]
                if (value.resolve() != null && o.file.clazz.findProperty(value.propPath()).isList) {
                    if (o.alias == null) {
                        val prop = value.text
                        o.error("An alias must be specified because the property `$prop` is a list association")
                    }
                }
            }

            if (propName == "flat") {
                val value = arg.values[0]
                if (dto notModifiedBy Modifier.Specification) {
                    if (value.resolve() != null && o.file.clazz.findProperty(value.propPath()).isList) {
                        o.error("`flat` can only handle collection associations in specific modified dto")
                    }
                }
            }
        }

        private fun visitProp(o: DTOPositiveProp, propName: String) {
            val availableProperties = o[StructureType.PropProperties]

            if (availableProperties.isEmpty()) {
                val upper = o.upper
                if (upper is DTOPositiveProp) {
                    upper.name.error("Prop `${upper.name.value}` does not exist")
                }
                return
            }

            val prop = availableProperties.find { it.name == propName } ?: let {
                o.name.error("`$propName` does not exist")
                return
            }
            if (prop.isEntityAssociation && o.body == null && o.recursive == null) {
                o.name.error("`$propName` must have child body")
            }
        }

        /**
         * 为属性配置上色
         */
        override fun visitPropConfig(o: DTOPropConfig) {
            val configName = o.name.text
            o.name.style(DTOSyntaxHighlighter.PROP_CONFIG)

            when (configName) {
                PropConfigName.Where.text -> {
                    val predicates = o.whereArgs?.predicates
                    if (predicates == null) {
                        o.name.error("!where accepts only predicates")
                    }
                }

                PropConfigName.OrderBy.text -> {
                    val orderItems = o.orderByArgs?.orderItems
                    if (orderItems == null) {
                        if (o.qualifiedName == null) {
                            o.name.error("!orderBy accepts only orderItems")
                        }
                    }
                }

                PropConfigName.Filter.text -> {
                    if (o.qualifiedName == null) {
                        o.name.error("!filter accepts only one identifier value")
                    }
                }

                PropConfigName.Recursion.text -> {
                    if (o.qualifiedName == null) {
                        o.name.error("!recursion accepts only one identifier value")
                    }
                }

                PropConfigName.FetchType.text -> {
                    val fetchType = o.qualifiedName
                    if (fetchType == null) {
                        o.name.error("!fetchType accepts only one identifier value")
                    } else {
                        fetchType.style(DTOSyntaxHighlighter.VALUE)

                        val fetchTypeValue = fetchType.value
                        if (fetchTypeValue !in DTOLanguage.availableFetchTypes) {
                            val availableTypes = DTOLanguage.availableFetchTypes.joinToString()
                            fetchType.error("Incorrect fetchType `$fetchTypeValue`, available types are: $availableTypes")
                        }
                    }
                }

                PropConfigName.Limit.text -> {
                    val intPair = o.intPair
                    if (intPair == null) {
                        o.name.error("!limit accepts only numeric value")
                    } else {
                        val limit = intPair.first
                        val limitValue = limit.text.toInt()
                        if (limitValue < 1) {
                            limit.error("limit cannot be less than 1")
                        }

                        val offset = intPair.second
                        if (offset != null) {
                            val offsetValue = offset.text.toInt()
                            if (offsetValue < 0) {
                                offset.error("offset cannot be less than 0")
                            }
                        }
                    }
                }

                PropConfigName.Batch.text -> {
                    val intPair = o.intPair
                    if (intPair == null) {
                        o.name.error("!batch accepts only numeric value")
                    } else {
                        val batch = intPair.first
                        val batchValue = batch.text.toInt()
                        if (batchValue < 1) {
                            batch.error("batch cannot be less than 1")
                        }

                        intPair.second?.error("!batch accepts only one numeric value")
                    }
                }

                PropConfigName.Depth.text -> {
                    val intPair = o.intPair
                    if (intPair == null) {
                        o.name.error("!depth accepts only numeric value")
                    } else {
                        val depth = intPair.first
                        val depthValue = depth.text.toInt()
                        if (depthValue < 0) {
                            depth.error("depth cannot be less than 1")
                        }

                        intPair.second?.error("!depth accepts only one numeric value")
                    }
                }

                else -> {
                    val availableNames = PropConfigName.availableNames
                    o.name.error("Incorrect prop-config name `$configName`, available names are: $availableNames")
                }
            }
        }

        /**
         * 为属性名称上色
         */
        override fun visitPropName(o: DTOPropName) {
            val parent = o.parent
            if (parent is DTOPositiveProp && parent.arg == null) {
                o.style(DTOSyntaxHighlighter.IDENTIFIER)
            }
        }

        /**
         * 为枚举映射体上色
         */
        override fun visitEnumBody(o: DTOEnumBody) {
            val prop = o.parent.parent<DTOPositiveProp>()
            val availableEnums = prop[StructureType.EnumValues]
            val currentEnumNames = o.mappings.map { it.constant.text }

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
                    GenerateMissedEnumMappings(prop.name.value, missedMappings, o),
                )
            }
        }

        /**
         * 为枚举映射上色
         */
        override fun visitEnumMapping(o: DTOEnumMapping) {
            val enumBody = o.parent<DTOEnumBody>()
            val prop = enumBody.parent.parent<DTOPositiveProp>()
            val enumMappingName = o.constant.text
            val enumMappingValue = o.string ?: o.int ?: run {
                o.constant.error("Missing value")
                return
            }

            val availableEnums = prop[StructureType.EnumValues]
            val currentEnumNames = enumBody.mappings.mapNotNull { it.constant.text }
            val currentEnumValues = enumBody.mappings.mapNotNull { it.string ?: it.int }

            val allInt = currentEnumValues.all { it.text.matches(Regex("\\d+")) }
            val allString = currentEnumValues.all { it.text.matches(Regex("\".*\"")) }
            val valueTypeValid = allInt || allString

            // 枚举映射是否存在于对应枚举中
            if (enumMappingName in availableEnums) {
                o.constant.style(DTOSyntaxHighlighter.ENUM_INSTANCE)
            } else {
                o.error(
                    "Illegal enum mapping `$enumMappingName`",
                    RemoveElement(enumMappingName, o),
                )
            }
            // 枚举映射是否重复定义
            if (currentEnumNames.count { it == enumMappingName } != 1) {
                o.error(
                    "Duplicated enum mapping `$enumMappingName`",
                    RemoveElement(enumMappingName, o),
                    DTOSyntaxHighlighter.DUPLICATION,
                )
            }
            // 枚举映射中的值是否重复定义
            if (currentEnumValues.count { it.text == enumMappingValue.text } != 1) {
                enumMappingValue.error(
                    "Illegal value of enum mapping `$enumMappingName`, its duplicated",
                    RemoveElement(enumMappingValue.text, enumMappingValue),
                    DTOSyntaxHighlighter.DUPLICATION,
                )
            }
            // 枚举映射值是否同一类型
            if (!valueTypeValid) {
                enumMappingValue.error(
                    "Illegal value. Integer value and String value cannot be mixed",
                    RemoveElement(enumMappingValue.text, enumMappingValue),
                )
            }
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
