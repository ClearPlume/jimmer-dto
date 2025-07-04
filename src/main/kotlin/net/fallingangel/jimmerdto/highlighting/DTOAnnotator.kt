package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.intention.CommonIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.diagnostic.PluginException
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.fix.*
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.GenericType
import net.fallingangel.jimmerdto.util.*
import org.babyfish.jimmer.sql.Id

/**
 * 部分代码结构的高亮
 */
class DTOAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(DTOAnnotatorVisitor(holder))
    }

    private class DTOAnnotatorVisitor(private val holder: AnnotationHolder) : DTOVisitor() {
        /**
         * 导包重复检测(普通导包语句)
         */
        override fun visitImportStatement(o: DTOImportStatement) {
            val file = o.file
            // 这里不考虑分组导入
            if (o.groupedImport != null) {
                return
            }
            val type = o.qualifiedName.simpleName
            val imports = file.imports.map(Pair<String, PsiClass>::first) + file.alias.map { (alias) -> alias.value }
            if (imports.count { it == type } > 1) {
                o.error(
                    "Conflicting import: imported name `$type` is ambiguous",
                    RemoveElement(type, o),
                )
            }
        }

        /**
         * 分组导包语句
         */
        override fun visitImportedType(o: DTOImportedType) {
            val project = o.project
            val import = o.parent.parent<DTOImportStatement>()
            val clazz = JavaPsiFacade.getInstance(project)
                    .findPackage(import.qualifiedName.value)
                    ?.classes
                    ?.find { it.name == o.type.value }
            if (clazz == null) {
                o.type.error("Unresolved reference: ${o.type.value}")
            } else {
                if (clazz.isAnnotationType) {
                    o.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }

            // 导包重复检测
            val type = o.alias?.value ?: o.type.value
            val imports = o.file.imports.map(Pair<String, PsiClass>::first) + o.file.alias.map { (alias) -> alias.value }
            if (imports.count { it == type } > 1) {
                o.error(
                    "Conflicting import: imported name `$type` is ambiguous",
                    RemoveElement(
                        type,
                        o.parent,
                        {
                            it as DTOGroupedImport
                            it.types.find { type -> type.startOffsetInParent == o.startOffsetInParent }!!
                        },
                        {
                            val comma = it.siblingComma(true)
                            if (comma == null) {
                                listOf(it.siblingComma(false)!!)
                            } else {
                                listOf(comma)
                            }
                        },
                    ),
                )
            }
        }

        /**
         * 为全限定类名上色
         */
        override fun visitQualifiedName(o: DTOQualifiedName) {
            // 枚举字面量
            if (o.parts.size == 2 && o.parent is DTOAnnotationSingleValue) {
                o.parts[1].style(DTOSyntaxHighlighter.ENUM_INSTANCE)
            }

            if (o.parentOfType<DTOPropConfig>() != null && o.parts.size >= 2) {
                val prop = o.parentOfType<DTOPositiveProp>() ?: return
                val propType = prop.property?.actualType as? LClass<*> ?: return
                val relationProp = propType.findPropertyOrNull(o.parts.dropLast(1).map { it.text }) ?: return
                val idView = propType.findPropertyOrNull(o.parts.map { it.text }) ?: return

                if (relationProp.isReference && relationProp.isEntityAssociation && idView.hasAnnotation(Id::class)) {
                    val old = "${relationProp.name}.${idView.name}"
                    val new = "${relationProp.name}Id"
                    o.error(
                        "Please replace `$old` to `$new`",
                        ReplaceIdAccessorToView(o, old, new),
                    )
                }
            }
        }

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

            if (o.part in listOf("like", "null", "desc", "asc")) {
                o.style(DTOSyntaxHighlighter.IDENTIFIER)
            }

            val resolved = o.resolve()
            if (resolved == null && !o.text.endsWith("Id") && o.parent.parent !is DTOTypeRef && o.text !in DTOLanguage.preludes) {
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
            } else if (o.parentOfType<DTOPropConfig>() != null) {
                resolved ?: return
                val target = LanguageProcessor.analyze(o.file).resolve(resolved)
                if (target is LProperty<*>) {
                    if (target.isEntityAssociation && !target.isReference) {
                        o.error("Illegal property: Table joins are not permitted here")
                    }
                }
            }
        }

        /**
         * Dto上色
         */
        override fun visitDto(o: DTODto) {
            // 修饰符上色
            val currentModifiers = o.modifierElements
            // 修饰符重复
            currentModifiers.forEach { modifier ->
                if (currentModifiers.count { it.text == modifier.text } != 1) {
                    modifier.error(
                        "Duplicated modifier `${modifier.text}`",
                        RemoveElement(modifier.text, modifier),
                        style = DTOSyntaxHighlighter.DUPLICATION,
                    )
                }
            }

            // `input` and `specification`
            if (o modifiedBy Modifier.Input && o modifiedBy Modifier.Specification) {
                currentModifiers.filter { it.text in listOf(Modifier.Specification.name.lowercase(), Modifier.Input.name.lowercase()) }
                        .forEach {
                            it.error(
                                "`input` and `specification` cannot appear at the same time",
                                RemoveElement(it.text, it),
                            )
                        }
            }

            // `unsafe` and `specification`
            if (o modifiedBy Modifier.Unsafe && o modifiedBy Modifier.Specification) {
                currentModifiers.filter { it.text in listOf(Modifier.Specification.name.lowercase(), Modifier.Unsafe.name.lowercase()) }
                        .forEach {
                            it.error(
                                "`unsafe` cannot be used with `specification`",
                                RemoveElement(it.text, it),
                            )
                        }
            }

            // `specification`只允许对实体使用
            if (o modifiedBy Modifier.Specification && !o.file.classIsEntity) {
                currentModifiers.find { it.text == Modifier.Specification.name.lowercase() }
                        ?.let {
                            it.error(
                                "`specification` can only be used to decorate entity type",
                                RemoveElement(it.text, it),
                            )
                        }
            }

            // InputStrategyModifier只允许针对input dto使用
            val inputModifiers = currentModifiers.zip(o.modifiers).filter { it.second.level == Modifier.Level.Both }
            if (o notModifiedBy Modifier.Input) {
                inputModifiers
                        .forEach { (element, _) ->
                            element.error(
                                "`${element.text}` can only be used for input",
                                RemoveElement(element.text, element),
                            )
                        }
            }

            // InputStrategyModifier只允许单个使用
            if (inputModifiers.size > 1) {
                inputModifiers
                        .forEach { (element, _) ->
                            element.error(
                                "InputStrategyModifier can only appear once",
                                RemoveElement(element.text, element),
                            )
                        }
                return
            }

            // 修饰符排序
            val orders = o.modifiers.map(Modifier::order)
            if (orders != orders.sorted()) {
                currentModifiers
                        .forEach {
                            it.fix(
                                DTOSyntaxHighlighter.WEAK_WARNING,
                                HighlightSeverity.WEAK_WARNING,
                                ProblemHighlightType.WEAK_WARNING,
                                "Non-canonical modifier order",
                                ReorderingModifier(o),
                            )
                        }
            }
        }

        /**
         * 为dto名称上色
         */
        override fun visitDtoName(o: DTODtoName) {
            // 重复的dto定义
            if (o.file.dtos.count { it == o.value } > 1) {
                o.error(
                    "Duplicated dto `${o.value}`",
                    RenameElement(o, Project::createDTOName),
                )
            }
        }

        /**
         * 为注解上色
         */
        override fun visitAnnotation(o: DTOAnnotation) {
            o.at.style(DTOSyntaxHighlighter.ANNOTATION)
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)

            val clazz = o.qualifiedName.clazz ?: return
            visitAnnotationName(o.qualifiedName, clazz)
            visitAnnotationParams(o, clazz, o.params, o.value)
        }

        /**
         * 为作为参数的注解上色
         */
        override fun visitNestAnnotation(o: DTONestAnnotation) {
            o.at?.style(DTOSyntaxHighlighter.ANNOTATION)
            o.qualifiedName.style(DTOSyntaxHighlighter.ANNOTATION)

            val clazz = o.qualifiedName.clazz ?: return
            visitAnnotationName(o.qualifiedName, clazz)
            visitAnnotationParams(o, clazz, o.params, o.value)
        }

        fun visitAnnotationName(name: DTOQualifiedName, clazz: PsiClass) {
            val qualifiedName = clazz.qualifiedName ?: return
            val `package` = qualifiedName.substringBeforeLast('.')

            if (name.simpleName in listOf("Nullable", "NonNull")) {
                name.error("Annotation \"Nullable\"、\"NonNull\" is forbidden")
            }

            if (name.simpleName in listOf("Null", "NotNull")) {
                val packages = listOf("javax.validation.constraints", "jakarta.validation.constraints")
                if (`package` !in packages) {
                    name.error("Package \"${`package`}\" is forbidden")
                }
            }

            if (`package`.startsWith("org.babyfish.jimmer") &&
                !`package`.startsWith("org.babyfish.jimmer.client") &&
                !`package`.startsWith("org.babyfish.jimmer.jackson") &&
                qualifiedName != "org.babyfish.jimmer.kt.dto.KotlinDto"
            ) {
                name.error("Jimmer annotation is forbidden")
            }
        }

        /**
         * @param value value参数
         */
        fun visitAnnotationParams(o: DTOElement, clazz: PsiClass, params: List<DTOAnnotationParameter>, value: DTOAnnotationValue?) {
            if (params.any { it.value == null }) {
                return
            }

            val haveValue = value != null

            // 必要参数是否给全
            val allParams = clazz.methods
                    .filterIsInstance<PsiAnnotationMethod>()
                    .filter { it.defaultValue == null }
                    .associateBy { it.name }
                    .toSortedMap()
            val currParams = params.map { it.name.text }.sorted()
            val notGivenParams = allParams - currParams - if (haveValue) listOf("value") else emptyList()
            if (notGivenParams.isNotEmpty()) {
                if (o is DTOAnnotation) {
                    o.qualifiedName.error(
                        "Not all the parameters required for `${o.qualifiedName.value}` are given",
                        GenerateMissingAnnotationParam(o, notGivenParams.values),
                    )
                } else {
                    o as DTONestAnnotation
                    o.qualifiedName.error(
                        "Not all the parameters required for `${o.qualifiedName.value}` are given",
                        GenerateMissingAnnotationParam(o, notGivenParams.values),
                    )
                }
            }

            // 参数类型是否匹配
            params
                    // 数组参数不以这种方式校验，其校验逻辑通过visitAnnotationArrayValue单独完成
                    .filter { it.value?.arrayValue == null }
                    .forEach { param ->
                        if (param.valueAssignableFromType) {
                            return@forEach
                        }

                        param.value?.let {
                            val type = param.type ?: return@forEach
                            val valueType = param.valueType ?: return@forEach
                            it.error("Required: `${type.canonicalText}`, Actual: `${valueType.canonicalText}`")
                        }
                    }

            if (value != null) {
                val method = clazz.findMethodsByName("value", false).first()
                val type = method.returnType ?: return

                val processor = LanguageProcessor.analyze(o.file)
                val valueType = processor.type(type, value)
                if (valueType != null && type.isAssignableFrom(valueType)) {
                    return
                }
                value.error("Required: `${type.canonicalText}`, Actual: `${valueType?.canonicalText}`")
            }
        }

        /**
         * 为注解无名参数上色
         */
        override fun visitAnnotationValue(o: DTOAnnotationValue) {
            val anno = o.parent
            if (anno is DTOAnnotation || anno is DTONestAnnotation) {
                val prevSibling = o.siblings(forward = false, withSelf = false)
                        .filter { it.elementType != TokenType.WHITE_SPACE }
                        .first()
                if (prevSibling.elementType != DTOLanguage.token[DTOParser.LParen]) {
                    val params = if (anno is DTOAnnotation) {
                        anno.params
                    } else {
                        anno as DTONestAnnotation
                        anno.params
                    }
                    if (params.any { it.value == null }) {
                        return
                    }
                    if (params.any { it.name.text == "value" }) {
                        return
                    }
                    o.error(
                        "value shorthand must be first or written as 'value = '",
                        MoveAnnotationParam(o),
                        AddValueParameterName(o),
                    )
                }
            }
        }

        /**
         * 为注解参数上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            if (o.value == null) {
                o.eq.error("Missing value after '='")
            }
            if (o.resolve() != null) {
                o.name.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
                o.eq.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            } else if (o.value != null) {
                val name = o.name.text
                o.name.error(
                    "No param with name '$name' found",
                    RemoveElement(
                        name,
                        o.parent,
                        { anno ->
                            anno.children
                                    .filterIsInstance<DTOAnnotationParameter>()
                                    .find { it.name.text == name }!!
                        },
                        { listOf(it.nextSibling) },
                    ),
                    SelectAnnotationParam(o),
                )
            }
        }

        /**
         * 为注解数组参数上色
         */
        override fun visitAnnotationArrayValue(o: DTOAnnotationArrayValue) {
            val parameter = o.parent.parentUnSure<DTOAnnotationParameter>() ?: return
            val arrayType = parameter.type as? PsiArrayType ?: return
            val arrayComponentType = arrayType.componentType
            val processor = LanguageProcessor.analyze(o.file)

            val arrayValue = parameter.value?.arrayValue ?: return
            val valueTypes = arrayValue.values.map { it to processor.type(arrayComponentType, it) }

            valueTypes.forEach { (value, type) ->
                type ?: return@forEach
                if (!arrayComponentType.isAssignableFrom(type)) {
                    value.error("Required: `${arrayComponentType.canonicalText}`, Actual: `${type.canonicalText}`")
                }
            }
        }

        /**
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            // 宏名称
            val macroName = o.name
            if (macroName.value in listOf("allScalars", "allReferences")) {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                macroName.error(
                    "Macro name should be \"allScalars\" or \"allReferences\"",
                    ChooseMacro(macroName),
                )
            }

            // 宏的重复定义
            val parent = o.parent
            val macros = if (parent is DTODtoBody) {
                parent.macros
            } else {
                parent.findChildren("/aliasGroupBody/macro")
            }
            if (macros.count { it.name.value == macroName.value } > 1) {
                o.error(
                    "Duplicated macro ${macroName.value}",
                    RemoveElement(macroName.value, o),
                )
            }

            // 宏可选标识在specification中不再需要
            val dto = o.parentOfType<DTODto>() ?: return
            o.optional?.let {
                if (dto modifiedBy Modifier.Specification) {
                    it.error(
                        "Unnecessary optional modifier `?`",
                        RemoveElement("?", it),
                    )
                }
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

            fun locateTarget(args: PsiElement, offset: Int): DTOMacroArg {
                args as DTOMacroArgs
                return args.values.find { it.startOffsetInParent == offset }!!
            }

            fun locateRelated(arg: PsiElement): List<PsiElement> {
                val args = arg.parent as DTOMacroArgs
                return if (args.values.indexOf(arg) == args.values.lastIndex) {
                    listOfNotNull(arg.siblingComma(false))
                } else {
                    listOfNotNull(arg.siblingComma())
                }
            }

            // 不允许出现超过一个<this>
            val thisList = argList.filter { it.text == "this" }
            thisList.forEach { it.style(DTOSyntaxHighlighter.KEYWORD) }
            if (thisList.size > 1) {
                thisList.forEach { `this` ->
                    `this`.error(
                        "Only one `this` is allowed",
                        RemoveElement(
                            "this",
                            o,
                            { locateTarget(it, `this`.startOffsetInParent) },
                            { locateRelated(it) },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION,
                    )
                }
            }

            // 宏可用参数，<this>一定是最后一个
            val macroAvailableParams = macro.types
            for (macroArg in argList) {
                // 当前元素不在宏可用参数中，即为非法
                if (macroArg.text !in macroAvailableParams) {
                    macroArg.error(
                        "Available parameters: [${macroAvailableParams.joinToString(", ")}]",
                        RemoveElement(
                            macroArg.text,
                            o,
                            { locateTarget(it, macroArg.startOffsetInParent) },
                            { locateRelated(it) },
                        )
                    )
                }
                // 当前元素在参数列表中出现过一次以上，即为重复
                if (argList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(
                        "Each parameter is only allowed to appear once",
                        RemoveElement(
                            macroArg.text,
                            o,
                            { locateTarget(it, macroArg.startOffsetInParent) },
                            { locateRelated(it) },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                }

                // 当前实体的简单类名和this同时出现
                // 当前实体的简单类名
                val thisName = macro.clazz.name

                // 等价于this的宏参数
                val sameThisArg = argList.find { it.text == thisName }
                if (macroArg.text == "this" && sameThisArg != null) {
                    sameThisArg.error(
                        "Here `$thisName` is equivalent to `this`",
                        RemoveElement(
                            sameThisArg.text,
                            o,
                            { locateTarget(it, sameThisArg.startOffsetInParent) },
                            { locateRelated(it) },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                    macroArg.error(
                        "Here `this` is equivalent to `$thisName`",
                        RemoveElement(
                            "this",
                            o,
                            { locateTarget(it, macroArg.startOffsetInParent) },
                            { locateRelated(it) },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                }
            }
        }

        /**
         * 为负属性上色
         */
        override fun visitNegativeProp(o: DTONegativeProp) {
            val name = o.name?.value ?: return
            // 属性存在性校验
            if (o.property != null) {
                o.style(DTOSyntaxHighlighter.NEGATIVE_PROP)
            } else {
                o.name?.error("`$name` does not exist")
            }

            val dtoBody = o.parent as DTODtoBody
            // 校验是否可使用负属性移除属性
            if (name !in dtoBody.availableProps) {
                o.name?.error("There is no `$name` that is need to be removed")
            }

            // 属性名称重复校验
            if (dtoBody.negativeProps.count { it.name?.value == name } > 1) {
                o.name?.error("Duplicated negative prop `$name`")
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
            o.name.style(DTOSyntaxHighlighter.IDENTIFIER)

            o.allSiblings(true).find { propName.value == it.name }?.let {
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
            if (o.parent is DTOUserProp) {
                visitUserPropType(o)
            }

            fun locateTarget(parent: PsiElement): PsiElement {
                return parent.children
                        .filterIsInstance<DTOTypeRef>()
                        .find { it.startOffsetInParent == o.startOffsetInParent }!!
            }

            fun locateRelated(type: PsiElement): List<PsiElement> {
                val comma = type.siblingComma()
                return if (comma == null) {
                    listOfNotNull(type.siblingComma(false))
                } else {
                    listOfNotNull(comma)
                }
            }

            val type = o.type.value
            val clazz = o.type.clazz

            // 类型解析
            if (clazz == null && type !in DTOLanguage.preludes) {
                o.type.error(
                    "Unresolved reference: $type",
                    ImportClass(o.type),
                )
                return
            }

            // 泛型校验
            val exceptedTypeParamNumber = GenericType[type]?.generics?.size ?: clazz?.typeParameters?.size ?: 0
            if ((o.arguments?.values?.size ?: 0) != exceptedTypeParamNumber) {
                o.type.error("Generic parameter mismatch")
            }

            // Dto接口实现校验
            val parent = o.parent
            if (parent is DTOImplements) {
                // 用作DTO、属性父级类型时，类型不可为空
                o.questionMark?.let {
                    it.error(
                        "Super interface type cannot be nullable",
                        RemoveElement("?", it),
                    )
                }

                // 重复实现类型
                if (parent.implements.count { it.type.value == o.type.value } > 1) {
                    o.error(
                        "Duplicate super interface `${o.type.value}`",
                        RemoveElement(o.type.value, o.parent, ::locateTarget, ::locateRelated),
                    )
                }
            }

            // 禁止类型校验
            if (clazz!!.qualifiedName!!.startsWith("org.babyfish.jimmer.")) {
                o.error(
                    "Types under `org.babyfish.jimmer` are not allowed",
                    RemoveElement(o.type.value, o.parent, ::locateTarget, ::locateRelated),
                )
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

            // 类型可空性校验
            val dto = o.parentOfType<DTODto>() ?: return
            if (o.questionMark == null && dto notModifiedBy Modifier.Specification && type !in DTOLanguage.preludes) {
                o.type.error("Type `${o.text}` is not null and its default value cannot be determined")
            }
        }

        /**
         * 为用户属性默认值上色
         */
        override fun visitDefaultValue(o: DTODefaultValue) {
            val prop = o.parent as DTOUserProp
            val type = prop.type
            val typeText = type.text
            val value = o.text

            // `null`值校验
            if (value == "null") {
                if (type.questionMark == null) {
                    o.error("`$value` does not match the type `$typeText`")
                }
                return
            }

            // 数字布尔字符串
            when (type.type.value) {
                "Boolean" -> if (!value.matches(Regex("true|false"))) {
                    o.error("`$value` does not match the type `$typeText`")
                }

                "Int" -> if (!value.matches(Regex("-?\\d+"))) {
                    o.error("`$value` does not match the type `$typeText`")
                }

                "String" -> if (!value.matches(Regex("\".*\""))) {
                    o.error("`$value` does not match the type `$typeText`")
                }

                "Float" -> if (!value.matches(Regex("-?\\d+\\.\\d+"))) {
                    o.error("`$value` does not match the type `$typeText`")
                }
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

        private fun visitFunction(o: DTOPositiveProp, functionName: String) {
            val dto = o.parentOfType<DTODto>() ?: return
            val specFunctions = SpecFunction.values().map(SpecFunction::expression)

            o.name.style(DTOSyntaxHighlighter.FUNCTION)
            val specification = dto modifiedBy Modifier.Specification
            // spec方法校验
            if (functionName in specFunctions && !specification) {
                o.error("Cannot call the function `$functionName` because the current dto type is not specification")
            }

            // 方法参数不可为空校验
            val arg = o.arg ?: return
            if (arg.isEmpty) {
                arg.error("Function arg list cannot be empty")
                return
            }

            // 方法参数是否存在校验
            arg.values.forEach {
                if (it.resolve() == null) {
                    it.error("`${it.text}` does not exist")
                }

                // 方法参数重复校验
                if (arg.values.count { value -> value.text == it.text } > 1) {
                    it.error(
                        "Duplicate prop `${it.text}`",
                        RemoveElement(
                            it.text,
                            arg,
                            { a ->
                                a as DTOPropArg
                                a.values.find { v -> v.startOffsetInParent == it.startOffsetInParent }!!
                            },
                            { a ->
                                val comma = a.siblingComma(false)
                                if (comma != null) {
                                    listOfNotNull(comma)
                                } else {
                                    listOfNotNull(a.siblingComma())
                                }
                            },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                }
            }

            // 方法参数数量验证
            val multiArgFunctions = SpecFunction.values().filter(SpecFunction::whetherMultiArg).map(SpecFunction::expression)
            if (arg.values.size > 1 && functionName !in multiArgFunctions) {
                arg.values
                        .drop(1)
                        .forEach {
                            it.error(
                                "`$functionName` accepts only one prop",
                                RemoveElement(it.text, it),
                            )
                        }
            }

            // 多方法参数别名校验
            if (functionName in multiArgFunctions) {
                if (arg.values.size > 1 && o.alias == null) {
                    o.error("An alias must be specified because `$functionName` has multiple arguments")
                }
            }

            // id方法参数为list时别名校验
            if (functionName == "id") {
                val value = arg.values[0]
                if (value.resolve() != null && o.file.clazz.findProperty(value.parent.parent.propPath() + value.text).isList) {
                    if (o.alias == null) {
                        val prop = value.text
                        o.error("An alias must be specified because the property `$prop` is a list association")
                    }
                }
            }

            // flat方法使用集合参数的校验
            if (functionName == "flat") {
                val value = arg.values[0]
                if (dto notModifiedBy Modifier.Specification) {
                    if (value.resolve() != null && o.file.clazz.findProperty(value.parent.parent.propPath()).isList) {
                        o.error("`flat` can only handle collection associations in specific modified dto")
                    }
                }
            }

            // like方法校验
            if (o.flag != null) {
                if (functionName != "like") {
                    o.name.error("`/` can only be used to decorate the function `like`")
                }

                val flag = o.flag!!
                if (flag.insensitive?.text != "i") {
                    flag.insensitive?.error("Illegal function option identifier `${flag.insensitive?.text}`, it can only be `i`")
                }
            }
        }

        private fun visitProp(o: DTOPositiveProp, propName: String) {
            // 父级属性是否存在
            val availableProperties = o.allSiblings()
            if (availableProperties.isEmpty()) {
                val upper = o.upper
                if (upper is DTOPositiveProp) {
                    try {
                        upper.name.error("Prop `${upper.name.value}` does not exist")
                    } catch (_: PluginException) {
                    }
                }
                return
            }

            // 属性是否存在
            val prop = availableProperties.find { it.name == propName } ?: let {
                o.name.error("`$propName` does not exist")
                return
            }

            // 关联属性需要指定body
            if (prop.isEntityAssociation && o.body == null && o.recursive == null) {
                o.name.error("`$propName` must have child body")
            }

            // as组中不允许直接子级使用as别名
            val alias = o.alias
            if (alias != null && o.parent.elementType == DTOLanguage.rule[DTOParser.RULE_aliasGroupBody]) {
                o.error(
                    "Alias definition for direct children is prohibited in `alias-group`",
                    RemoveElement(
                        "Alias `${alias.text}` for $propName",
                        o,
                        {
                            it as DTOPositiveProp
                            it.alias!!
                        },
                        {
                            it as DTOPositiveProp
                            listOfNotNull(it.`as`)
                        },
                    ),
                )
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
            // 有些属性名称可能是关键字颜色，需要覆盖掉
            if (parent is DTOPositiveProp && parent.arg == null) {
                o.style(DTOSyntaxHighlighter.IDENTIFIER)
            }

            // 属性名称重复校验
            val dtoBody = if (parent.parent is DTODtoBody) {
                parent.parent as DTODtoBody
            } else {
                parent.parent.parent.parent as DTODtoBody
            }
            val name = if (parent is DTOPositiveProp) {
                parent.alias?.value ?: parent.name.value
            } else {
                o.value
            }

            if (dtoBody.parent is DTOPropBody) {
                val prop = dtoBody.parent.parent as DTOPositiveProp
                if (prop.name.value == "files") {
                    val dto = prop.parentOfType<DTODto>()
                    if (dto != null && dto.name.value == "UserTest") {
                        println("props: ${dtoBody.existedProp}")
                    }
                }
            }

            dtoBody.existedProp[name]?.let { (count, alias) ->
                if (count > 1) {
                    o.error("Duplicated name usage `$name`")
                }
            }
        }

        /**
         * 为枚举映射体上色
         */
        override fun visitEnumBody(o: DTOEnumBody) {
            val prop = o.parent.parent<DTOPositiveProp>()
            val availableEnums = o.values
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
            val enumMappingName = o.constant.text
            val enumMappingValue = o.string ?: o.int ?: run {
                o.constant.error("Missing value")
                return
            }

            val availableEnums = enumBody.values
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
                    style = DTOSyntaxHighlighter.DUPLICATION,
                )
            }
            // 枚举映射中的值是否重复定义
            if (currentEnumValues.count { it.text == enumMappingValue.text } != 1) {
                enumMappingValue.error(
                    "Illegal value of enum mapping `$enumMappingName`, its duplicated",
                    RemoveElement(enumMappingValue.text, enumMappingValue),
                    style = DTOSyntaxHighlighter.DUPLICATION,
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

        private fun PsiElement.annotator(style: TextAttributesKey, severity: HighlightSeverity) {
            holder.newSilentAnnotation(severity)
                    .range(this)
                    .textAttributes(style)
                    .create()
        }

        private fun PsiElement.error(
            message: String,
            vararg fixes: CommonIntentionAction,
            style: TextAttributesKey = DTOSyntaxHighlighter.ERROR,
            highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR
        ) {
            fix(style, HighlightSeverity.ERROR, highlightType, message, *fixes)
        }

        private fun PsiElement.fix(
            style: TextAttributesKey,
            severity: HighlightSeverity,
            highlightType: ProblemHighlightType,
            message: String,
            vararg fixes: CommonIntentionAction,
        ) {
            val fixerBuilder = holder.newAnnotation(severity, message)
            fixerBuilder
                    .range(this)
                    .textAttributes(style)
                    .highlightType(highlightType)
            fixes.forEach { fix ->
                fixerBuilder.withFix(fix)
            }
            fixerBuilder.create()
        }
    }
}
