package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.intention.CommonIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
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
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.LanguageProcessor
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.lsi.jimmer.isEntityAssociation
import net.fallingangel.jimmerdto.lsi.jimmer.isList
import net.fallingangel.jimmerdto.lsi.jimmer.isReference
import net.fallingangel.jimmerdto.lsi.jimmer.resolvedLClass
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.fix.*
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.structure.GenericType
import net.fallingangel.jimmerdto.util.*
import org.babyfish.jimmer.sql.Id
import org.jetbrains.kotlin.idea.base.psi.childrenDfsSequence
import org.jetbrains.kotlin.psi.KtClass

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
            // 这里不考虑分组导入
            if (o.groupedImport != null) {
                return
            }
            val type = o.qualifiedName.simpleName
            if ((o.file.importIndex[type]?.size ?: 0) > 1) {
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
            val clazz = o.type.target?.source
            if (clazz == null) {
                o.type.error("Unresolved reference: ${o.type.value}")
            } else {
                if ((clazz as? PsiClass)?.isAnnotationType == true || (clazz as? KtClass)?.isAnnotation() == true) {
                    o.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }

            // 导包重复检测
            val type = o.alias?.value ?: o.type.value
            if ((o.file.importIndex[type]?.size ?: 0) > 1) {
                o.error(
                    "Conflicting import: imported name `$type` is ambiguous",
                    RemoveElement(
                        type,
                        o.parent,
                        {
                            it as DTOGroupedImport
                            it.types.find { type -> type.startOffsetInParent == o.startOffsetInParent }!!
                        },
                        { listOfNotNull(it.siblingComma(false), it.siblingComma()) },
                    ),
                )
            }
        }

        /**
         * 为全限定类名上色
         */
        override fun visitQualifiedName(o: DTOQualifiedName) {
            if (o.parentOfType<DTOPropConfig>() != null && o.parts.size >= 2) {
                val prop = o.parentOfType<DTOPositiveProp>() ?: return
                val propClass = prop.property?.actualType?.resolvedLClass ?: return
                val relationProp = propClass.findProperty(o.parts.dropLast(1).map { it.text }) ?: return
                val idView = propClass.findProperty(o.parts.map { it.text }) ?: return

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
         * 
         * TODO 实体注解校验 the "Probe" is not decorated by "@Entity", "Embeddable" or "Immutable"
         */
        override fun visitQualifiedNamePart(o: DTOQualifiedNamePart) {
            if (o.part in DTOLanguage.softKeywords) {
                o.style(DTOSyntaxHighlighter.IDENTIFIER)
            }

            when (val target = o.target) {
                null -> {
                    // 在 `export a.b.c -> package a.b.c.d.e.f` 下的包不参与存在性校验
                    val underPackage = o.parent<DTOExportStatement> { `package` != null } == null
                    if (underPackage && (o.prevPart == null || o.prevPart?.target != null)) {
                        o.error("Unresolved reference: ${o.part}")
                    }
                }

                is Resolution.Target.EnumConst -> o.style(DTOSyntaxHighlighter.ENUM_INSTANCE)

                is Resolution.Target.Pkg -> {
                    val qualifiedName = o.qualifiedName ?: return
                    val importStatement = qualifiedName.parent as? DTOImportStatement ?: return
                    if (importStatement.groupedImport == null && qualifiedName.parts.last() === o) {
                        val packageAction = when {
                            o.haveParent<DTOExportStatement>() -> "exported"
                            o.haveParent<DTOImportStatement>() -> "imported"
                            else -> null
                        }

                        if (packageAction != null) {
                            o.error("Packages cannot be $packageAction")
                        }
                    }
                }

                is Resolution.Target.Type -> {
                    if ((target.source as? PsiClass)?.isAnnotationType == true || (target.source as? KtClass)?.isAnnotation() == true) {
                        o.style(DTOSyntaxHighlighter.ANNOTATION)
                    }
                }

                is Resolution.Target.Property -> {
                    val property = target.property
                    if (property.isEntityAssociation && !property.isReference) {
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
            if (o modifiedBy Modifier.Specification && !o.classIsEntity) {
                currentModifiers.find { it.text == Modifier.Specification.name.lowercase() }
                    ?.let {
                        it.error(
                            "`specification` can only be used to decorate entity type",
                            RemoveElement(it.text, it),
                        )
                    }
            }

            // InputStrategyModifier只允许针对input dto使用
            // TODO: Level 分类有误——Both 实际含义是 InputStrategyModifier（fixed/static/dynamic/fuzzy），
            //  不是"Dto 和 Prop 都能用"。Variant 是独立维度（morphism 的 default），不属于 Dto/Prop 轴。
            //  需要重新设计 Level 枚举，让每个修饰符的合法位置精确表达。
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

            // sealed 不允许在 Specification dto 上使用
            if (o modifiedBy Modifier.Specification) {
                val sealed = o.modifierElements.find { it.text == Modifier.Sealed.name.lowercase() }
                sealed?.error("The modifier 'sealed' is not allowed on specification")
            }

            // sealed 要求 dto 内部包含 #types 块
            if (o modifiedBy Modifier.Sealed) {
                if (o.childrenDfsSequence().none { it is DTOPolymorphic }) {
                    val sealed = o.modifierElements.find { it.text == Modifier.Sealed.name.lowercase() }!!
                    sealed.error("The modifier 'sealed' requires a #types block")
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

            if (name.simpleName in setOf("Nullable", "NonNull")) {
                name.error("Annotation `Nullable`、`NonNull` is forbidden")
            }

            if (name.simpleName in setOf("Null", "NotNull")) {
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
            val haveValue = value != null

            // 必要参数是否给全
            val allParams = clazz.methods
                .filterIsInstance<PsiAnnotationMethod>()
                .filter { it.defaultValue == null }
                .associateBy { it.name }
                .toSortedMap()
            val currParams = params.map { it.name.text }.sorted()
            val notGivenParams = allParams - currParams.toSet() - if (haveValue) setOf("value") else emptySet()
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

            val processor = LanguageProcessor.analyze(o.file)

            // 参数类型是否匹配
            params
                .forEach { param ->
                    val value = param.value ?: return@forEach
                    val type = param.type ?: return@forEach
                    validateAnnotationValues(type, value, processor)
                }

            if (value != null) {
                val method = clazz.findMethodsByName("value", false).firstOrNull()
                if (method == null) {
                    value.error(
                        "Annotation `@${clazz.name}` does not have a parameter named `value`, so the value cannot be abbreviated",
                        RemoveElement(
                            value.text,
                            value,
                            relatedElementsFinder = { listOfNotNull(it.siblingComma(true), it.siblingComma(false)) },
                        ),
                    )
                } else {
                    val type = method.returnType ?: return
                    validateAnnotationValues(type, value, processor)
                }
            }
        }

        private fun validateAnnotationValues(type: PsiType, value: DTOAnnotationValue, processor: LanguageProcessor) {
            val (expectedComponentType, isArrayExpected) = if (type is PsiArrayType) {
                type.componentType to true
            } else {
                type to false
            }

            val values = value.arrayValue?.values?.map { it to processor.type(it) }
                ?: value.let { listOf(it to processor.type(it)) }

            if (!isArrayExpected && values.size > 1) {
                value.error("Required: `${type.canonicalText}`, Actual: `${PsiArrayType(PsiTypes.voidType())}`")
                return
            }

            values.forEach { (value, valueType) ->
                valueType ?: return@forEach
                if (!expectedComponentType.isAssignableFrom(valueType)) {
                    value.error("Required: `${expectedComponentType.canonicalText}`, Actual: `${valueType.canonicalText}`")
                }
            }
        }

        /**
         * 为注解无名参数上色
         */
        override fun visitAnnotationValue(o: DTOAnnotationValue) {
            val anno = o.parent
            if (anno is DTOAnnotation || anno is DTONestAnnotation) {
                val prevSibling = o.siblings(forward = false, withSelf = false)
                    .first { it.elementType != TokenType.WHITE_SPACE }
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
         * 为宏名称上色
         */
        override fun visitMacro(o: DTOMacro) {
            // 宏名称
            val macroName = o.name
            // TODO 优化可选项获取逻辑
            val availableMacros = if (o.haveParent<DTOPolymorphic>()) {
                listOf("exhaustive")
            } else {
                listOf("allScalars", "allReferences")
            }

            if (macroName.value in availableMacros) {
                o.firstChild.style(DTOSyntaxHighlighter.MACRO)
                macroName.style(DTOSyntaxHighlighter.MACRO)
            } else {
                macroName.error(
                    "Macro name should be one of: $availableMacros",
                    ChooseMacro(macroName, availableMacros),
                )
                return
            }

            val parent = o.parent
            val macros = if (parent is DTODtoBody) {
                parent.macros
            } else {
                parent.findChildren("/aliasGroupBody/macro")
            }

            // 宏的定义应该在第一位
            val siblings = o.siblings(forward = false, withSelf = false).filterIsInstance<DTOElement>()
            // TODO 结构修正
            if (siblings.any { it !is DTOMacro } && macroName.value != "exhaustive") {
                o.error("Macro must be declared before any other elements")
            }

            // 宏的重复定义
            if (macros.count { it.name.value == macroName.value } > 1) {
                o.error(
                    "Duplicated macro ${macroName.value}",
                    RemoveElement(macroName.value, o),
                )
                return
            }

            // 宏可选标识在specification中不再需要
            val dto = o.parentOfType<DTODto>() ?: return
            o.optional?.let {
                if (dto modifiedBy Modifier.Specification) {
                    it.error(
                        "Unnecessary optional modifier `?`",
                        RemoveElement("?", it),
                    )
                    return
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
                return listOfNotNull(arg.siblingComma(false), arg.siblingComma())
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
                val thisName = macro.containingLClass?.name ?: return

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
            if (o.containingLClass?.findProperty(name) != null) {
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

            val power = o.power
            val original = o.original
            val dollar = o.dollar
            val arrow = o.arrow
            val replacement = o.replacement

            if (arrow == null) {
                o.error("The '->' is required in alias group", InsertArrow(o))
                return
            }

            if (original == null && replacement == null) {
                arrow.error("There is no identifier to the left or right of the '->'")
                return
            }

            if (power == null && original == null && dollar == null) {
                arrow.error("There is nothing to the left of the '->', which is not allowed")
                return
            }

            if (power != null && dollar != null) {
                power.error(
                    "The `^` and `$` cannot appear at the same time",
                    RemoveElement("^", power)
                )
                dollar.error(
                    "The `^` and `$` cannot appear at the same time",
                    RemoveElement("$", dollar)
                )
                return
            }
        }

        /**
         * 为用户属性上色
         */
        override fun visitUserProp(o: DTOUserProp) {
            val propName = o.name
            o.name.style(DTOSyntaxHighlighter.IDENTIFIER)

            val parentFlat = o.parent<DTOPositiveProp> { name.value == "flat" }
            if (parentFlat != null) {
                o.error(
                    "User defined property cannot be declared under flat type",
                    RemoveElement(o.name.value, o),
                )
                return
            }

            o.containingLClass?.findProperty(propName.name)?.let {
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
                return listOfNotNull(type.siblingComma(false), type.siblingComma())
            }

            val type = o.type.value
            val clazz = o.type.target?.source
            val (typeParameterNumber, qualifiedName) = when (clazz) {
                is PsiClass -> clazz.typeParameters.size to clazz.qualifiedName
                is KtClass -> clazz.typeParameters.size to clazz.fqName?.asString()
                else -> 0 to ""
            }

            // 类型解析
            if (clazz == null && type !in DTOLanguage.preludes) {
                o.type.error(
                    "Unresolved reference: $type",
                    ImportClass(o.type),
                )
                return
            }

            // 泛型校验
            val exceptedTypeParamNumber = GenericType[type]?.generics?.size ?: typeParameterNumber
            if ((o.arguments?.values?.size ?: 0) != exceptedTypeParamNumber) {
                o.type.error("Generic parameter mismatch, expected `$exceptedTypeParamNumber` but got `${o.arguments?.values?.size ?: 0}`")
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
            if (qualifiedName?.startsWith("org.babyfish.jimmer.") == true) {
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
            // 方法存在性校验
            val function = Function.entries.find { it.expression == functionName }

            if (function == null) {
                o.name.error("Unknown function `$functionName`")
                return
            } else {
                o.name.style(DTOSyntaxHighlighter.FUNCTION)
            }

            // fold 校验
            if (function == Function.Fold) {
                if (o.haveParent<DTOAliasGroup>()) {
                    o.name.error("`fold` cannot be used inside alias group", RemoveElement("fold", o))
                }
                // 方法参数不可为空校验
                val arg = o.arg ?: return
                if (arg.isEmpty) {
                    arg.error("Function arg list cannot be empty")
                    return
                }
                // fold 没有后续校验需求
                return
            }

            // 方法参数不可为空校验
            val arg = o.arg ?: return
            if (arg.isEmpty) {
                arg.error("Function arg list cannot be empty")
                return
            }

            val dto = o.parentOfType<DTODto>() ?: return

            // Spec 方法校验
            if (functionName == "id" && dto modifiedBy Modifier.Specification) {
                o.name.error(
                    "`id` is forbidden by specification, replace it to `associatedIdEq`",
                    ReplaceName(o.name, "associatedIdEq", Project::createPropName),
                )
            }

            if (function.whetherSpec && dto notModifiedBy Modifier.Specification) {
                o.name.error("Cannot call the function `$functionName` because the current dto type is not specification")
                return
            }

            // 方法参数校验
            arg.values.forEach {
                // 方法参数是否存在校验
                if (it.property == null) {
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
                            { a -> listOfNotNull(a.siblingComma(false), a.siblingComma()) },
                        ),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                }
            }

            // 方法参数数量验证
            if (arg.values.size > 1 && !function.whetherMultiArg) {
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
            if (function.whetherMultiArg) {
                if (arg.values.size > 1 && o.alias == null) {
                    o.error("An alias must be specified because `$functionName` has multiple arguments")
                }
            }

            // id方法参数为list时别名校验
            if (function == Function.Id) {
                val value = arg.values[0]
                if (value.property?.isList == true) {
                    if (o.alias == null) {
                        val prop = value.text
                        o.error("An alias must be specified because the property `$prop` is a list association")
                    }
                }
            }

            // flat方法使用集合参数的校验
            if (function == Function.Flat) {
                val value = arg.values[0]
                if (dto notModifiedBy Modifier.Specification) {
                    if (value.property?.isList == true) {
                        o.error("`flat` can only handle collection associations in specific modified dto")
                    }
                }
            }

            // like方法校验
            val flag = o.flag
            if (flag != null) {
                if (function != Function.Like) {
                    o.name.error("`/` can only be used to decorate the function `like`", RemoveElement(flag.text, flag))
                }

                val insensitive = flag.insensitive
                if (insensitive != null && insensitive.text != "i") {
                    insensitive.error(
                        "Illegal function option identifier `${insensitive.text}`, it can only be `i`",
                        ReplaceName(insensitive, "i", newElement = { insensitive.project.createInsensitive() }),
                    )
                }
            }
        }

        private fun visitProp(o: DTOPositiveProp, propName: String) {
            val availableProperties = o.containingLClass?.allProperties ?: return

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

            // TODO 属性名称重复校验
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
