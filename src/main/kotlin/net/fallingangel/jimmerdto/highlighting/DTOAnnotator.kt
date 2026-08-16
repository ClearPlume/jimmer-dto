package net.fallingangel.jimmerdto.highlighting

import com.intellij.codeInsight.intention.CommonIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.enums.*
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.compiling
import net.fallingangel.jimmerdto.lsi.jimmer.*
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.fix.*
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.*
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
         * TODO export 处实体注解校验 the "Probe" is not decorated by "@Entity", "Embeddable" or "Immutable"
         */
        override fun visitExportStatement(o: DTOExportStatement) {
        }

        /**
         * 导包重复检测(普通导包语句)
         */
        override fun visitImportStatement(o: DTOImportStatement) {
            // 这里不考虑分组导入
            if (o.groupedImport != null) {
                return
            }

            // 重复导入
            val type = o.simpleName
            if ((o.file.importIndex[type]?.size ?: 0) > 1) {
                o.error(
                    "Conflicting import: imported name `$type` is ambiguous",
                    RemoveElement(type, o),
                )
            }

            // 内置类型不可导入
            val qualifiedType = o.qualifiedName.value
            if (qualifiedType in AUTO_IMPORTED_TYPES) {
                o.error("'$qualifiedType' cannot be imported because it is built-in type", RemoveElement(qualifiedType, o))
            }

            // 注解别名高亮
            val target = o.qualifiedName.target
            if (target is Resolution.Target.Type) {
                if (process(target.type) { isAnnotationClass() } == true) {
                    o.alias?.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }
        }

        /**
         * 分组导包语句
         */
        override fun visitImportedType(o: DTOImportedType) {
            val clazz = o.type.target?.source
            if (clazz == null) {
                o.type.error("Unresolved reference: ${o.type.value}")
                return
            } else {
                if (process(clazz) { isAnnotationClass() } == true) {
                    o.type.style(DTOSyntaxHighlighter.ANNOTATION)
                    o.alias?.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }

            // 导包重复检测
            val type = o.simpleName
            if ((o.file.importIndex[type]?.size ?: 0) > 1) {
                o.error(
                    "Conflicting import: imported name `$type` is ambiguous",
                    RemoveElement(type, o),
                )
            }

            // 内置类型不可导入
            val qualifiedType = process(clazz) { classQualifiedName() } ?: return
            if (qualifiedType in AUTO_IMPORTED_TYPES) {
                o.error(
                    "'$qualifiedType' cannot be imported because it is built-in type",
                    RemoveElement(qualifiedType, o),
                )
            }
        }

        /**
         * 为全限定类名的部分上色
         */
        override fun visitQualifiedNamePart(o: DTOQualifiedNamePart) {
            if (o.part in DTOLanguage.softKeywords) {
                o.style(DTOSyntaxHighlighter.IDENTIFIER)
            }

            // 内置类型：解析目标可能不存在（Java 侧 Array 无对应类），存在性由表回答
            if (o.prevPart == null && o.qualifiedName.parts.size == 1 && StandardType[o.part] != null) {
                return
            }

            fun styleIfAnnotation(source: PsiElement) {
                if (process(source) { isAnnotationClass() } == true) {
                    o.style(DTOSyntaxHighlighter.ANNOTATION)
                }
            }

            when (val target = o.target) {
                null -> {
                    // 在 `export a.b.c -> package a.b.c.d.e.f` 下的包不参与存在性校验
                    val notUnderExportPackage = o.parent<DTOExportStatement> { `package` != null } == null
                    if (notUnderExportPackage && (o.prevPart == null || o.prevPart?.target != null)) {
                        val fixes = mutableListOf<CommonIntentionAction>()
                        if (o.space is Resolution.Space.GlobalWithImports) {
                            fixes.add(ImportClassFix(o))
                        }
                        o.error("Unresolved reference: ${o.part}", *fixes.toTypedArray())
                    }
                }

                is Resolution.Target.EnumConst -> o.style(DTOSyntaxHighlighter.ENUM_INSTANCE)

                is Resolution.Target.Pkg -> {
                    val importStatement = o.qualifiedName.parent as? DTOImportStatement ?: return
                    if (importStatement.groupedImport == null && o.qualifiedName.parts.last() === o) {
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

                is Resolution.Target.Type -> styleIfAnnotation(target.source)

                is Resolution.Target.Property -> {
                    val property = target.property
                    if (property.isEntityAssociation && !property.isReference) {
                        o.error("Illegal property: Table joins are not permitted here")
                    }
                }

                is Resolution.Target.Subtype -> {
                }

                is Resolution.Target.Alias -> target.target?.let { styleIfAnnotation(target.target.source) }
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
            visitAnnotation(o as DTOAnnotationElement)
        }

        /**
         * 为作为参数的注解上色
         */
        override fun visitNestAnnotation(o: DTONestAnnotation) {
            o.at?.style(DTOSyntaxHighlighter.ANNOTATION)
            visitAnnotation(o)
        }

        fun visitAnnotation(annotation: DTOAnnotationElement) {
            visitAnnotationName(annotation.qualifiedName)
            visitParamNames(annotation)
            visitParamValues(annotation)
        }

        fun visitAnnotationName(qualifiedName: DTOQualifiedName) {
            val type = (qualifiedName.target as? Resolution.Target.Type)?.type ?: return
            val annotationName = process(type) { classQualifiedName() } ?: return
            val simpleName = qualifiedName.simpleName

            if (simpleName in setOf("Nullable", "NonNull")) {
                qualifiedName.error("Annotation '$simpleName' is reserved by DTO language")
                return
            }

            if (simpleName in setOf("Null", "NotNull")) {
                val packages = setOf("javax.validation.constraints", "jakarta.validation.constraints")
                if (annotationName.substringBeforeLast('.') !in packages) {
                    qualifiedName.error("Only ${packages.joinToString { "'$it.$simpleName'" }} are accepted by DTO language")
                }
                return
            }

            if (simpleName == "TNullable" && annotationName != JimmerAnnotations.TNullable.asFqNameString()) {
                qualifiedName.error("Only '${JimmerAnnotations.TNullable.asFqNameString()}' is accepted by DTO language")
                return
            }

            if (annotationName.startsWith("org.babyfish.jimmer.") &&
                !annotationName.startsWith("org.babyfish.jimmer.client.") &&
                !annotationName.startsWith("org.babyfish.jimmer.jackson.") &&
                annotationName != "org.babyfish.jimmer.kt.dto.KotlinDto"
            ) {
                qualifiedName.error("Jimmer annotation '$annotationName' is forbidden by DTO language")
                return
            }
        }

        fun visitParamNames(annotation: DTOAnnotationElement) {
            val writtenNames = buildList {
                annotation.value?.let { add("value" to it) }
                annotation.params.forEach { add(it.name.text to it.name) }
            }

            writtenNames.groupBy { it.first }.values
                .filter { it.size > 1 }
                .flatten()
                .forEach { (name, element) ->
                    if (element is DTOAnnotationValue) {
                        element.error(
                            "Shorthand form is the `value` parameter, which duplicates the explicit `value`",
                            RemoveElement(element.text, element),
                        )
                    } else {
                        element.error(
                            "Duplicated annotation parameter `$name`",
                            RemoveElement(element.parent.text, element.parent),
                        )
                    }
                }
        }

        fun visitParamValues(annotation: DTOAnnotationElement) {
            val declaredParams = annotation.lAnnotation?.params ?: return
            val declaredNames = declaredParams.mapTo(mutableSetOf()) { it.name }

            data class ParamEntry(val name: String, val nameElement: PsiElement, val value: DTOAnnotationValue?)

            val writtenParams = buildList {
                annotation.value?.let { add(ParamEntry("value", it, it)) }
                annotation.params.forEach { add(ParamEntry(it.name.text, it.name, it.value)) }
            }

            // 使用处定义处没有的参数
            writtenParams.forEach { (name, nameElement) ->
                if (name !in declaredNames) {
                    nameElement.error(
                        "No parameter with name '$name' found",
                        RemoveElement(name, nameElement as? DTOAnnotationValue ?: nameElement.parent),
                    )
                }
            }

            // 使用处没有写全必需参数
            val writtenNames = writtenParams.mapTo(mutableSetOf()) { it.name }
            val missedParams = declaredParams.filter { it.defaultValue == null && it.name !in writtenNames }
            missedParams
                .forEach { param ->
                    val fixes = buildList {
                        add(AddAnnotationParam(annotation, param))
                        if (missedParams.size > 1) {
                            add(AddAllAnnotationParams(annotation, missedParams))
                        }
                    }
                    annotation.qualifiedName.error("Missing required parameter '${param.name}'", *fixes.toTypedArray())
                }

            // 参数类型校验
            writtenParams.forEach { (name, nameElement, valueElement) ->
                val param = declaredParams.firstOrNull { it.name == name } ?: return@forEach
                val value = valueElement?.value ?: return@forEach

                if (!param.accepts(value)) {
                    nameElement.error("Type mismatch: inferred type is '${value.typeName}' but '${param.type.presentation}' was expected")
                }
            }
        }

        /**
         * 为注解无名参数上色
         */
        override fun visitAnnotationValue(o: DTOAnnotationValue) {
            val annotation = o.parent as? DTOAnnotationElement ?: return

            // value 简写应该在第一位
            val siblings = o.siblings(forward = false, withSelf = false).filterIsInstance<DTOAnnotationParameter>()
            if (siblings.any()) {
                o.error(
                    "Value shorthand must be the first parameter",
                    MoveValueToFirst(annotation),
                    AddValueParameterName(o),
                )
            }
        }

        /**
         * 为注解参数上色
         */
        override fun visitAnnotationParameter(o: DTOAnnotationParameter) {
            if (o.value == null) {
                o.eq.error("Expecting an argument")
            }
            o.name.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
            o.eq.style(DTOSyntaxHighlighter.NAMED_PARAMETER_NAME)
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

            // 不允许出现超过一个<this>
            val thisList = argList.filter { it.text == "this" }
            thisList.forEach { it.style(DTOSyntaxHighlighter.KEYWORD) }
            if (thisList.size > 1) {
                thisList.forEach {
                    it.error(
                        "Only one `this` is allowed",
                        RemoveElement("this", it),
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
                        RemoveElement(macroArg.text, macroArg)
                    )
                }
                // 当前元素在参数列表中出现过一次以上，即为重复
                if (argList.count { it.text == macroArg.text } != 1) {
                    macroArg.error(
                        "Each parameter is only allowed to appear once",
                        RemoveElement(macroArg.text, macroArg),
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
                        RemoveElement(sameThisArg.text, sameThisArg),
                        style = DTOSyntaxHighlighter.DUPLICATION
                    )
                    macroArg.error(
                        "Here `this` is equivalent to `$thisName`",
                        RemoveElement("this", macroArg),
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
                o.name?.style(DTOSyntaxHighlighter.NEGATIVE_PROP)
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
            o.name.style(DTOSyntaxHighlighter.IDENTIFIER)

            val parentFlat = o.parent<DTOPositiveProp> { name.value == "flat" }
            if (parentFlat != null) {
                o.error(
                    "User defined property cannot be declared under flat type",
                    RemoveElement(o.name.value, o),
                )
                return
            }

            o.containingLClass?.findProperty(o.name.value)?.let {
                o.name.error(
                    "It is prohibited for user-prop and entity prop to have the same name",
                    RenameElement(o.name, Project::createPropName),
                )
            }

            val type = o.type
            val equals = o.equals
            val defaultValue = o.defaultValue

            if (type == null) {
                o.name.error("Type is required for user property")
                return
            }

            visitUserPropType(type)

            if (equals == null) {
                if (defaultValue != null) {
                    type.error("Missing '=' before default value")
                }
                return
            }

            if (defaultValue == null) {
                equals.error("Missing default value after '='")
                return
            }

            visitDefaultValue(type, defaultValue)
        }

        private fun visitUserPropType(o: DTOTypeRef) {
            if (o.type.parts.size > 1 || o.type.target == null) {
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
            if (o.questionMark == null && dto notModifiedBy Modifier.Specification && StandardType[type] == null) {
                o.type.error("Type `${o.text}` is not null and its default value cannot be determined")
            }
        }

        fun visitDefaultValue(type: DTOTypeRef, value: DTODefaultValue) {
            val typeText = type.text
            val valueText = value.text

            // `null`值校验
            if (valueText == "null") {
                if (type.questionMark == null) {
                    type.error("`$valueText` does not match the type `$typeText`")
                }
                return
            }

            // 数字布尔字符串
            when (typeText) {
                "Boolean" -> if (!valueText.matches(Regex("true|false"))) {
                    value.error("`$valueText` does not match the type `$typeText`")
                }

                "Int" -> if (!valueText.matches(Regex("-?\\d+"))) {
                    value.error("`$valueText` does not match the type `$typeText`")
                }

                "String" -> if (!valueText.matches(Regex("\".*\""))) {
                    value.error("`$valueText` does not match the type `$typeText`")
                }

                "Float" -> if (!valueText.matches(Regex("-?\\d+\\.\\d+"))) {
                    value.error("`$valueText` does not match the type `$typeText`")
                }
            }
        }

        /**
         * 为类型定义上色
         */
        override fun visitTypeDef(o: DTOTypeRef) {
            if (o.parent is DTOUserProp) {
                return
            }

            val type = o.type.value
            val clazz = o.type.target?.source
            val qualifiedName = clazz?.let { process(it) { classQualifiedName() } }

            // 泛型校验
            val declaredArity = when (clazz) {
                is PsiClass -> clazz.typeParameters.size
                is KtClass -> clazz.typeParameters.size
                else -> 0
            }
            val exceptedTypeParamNumber = StandardType[type]?.arity ?: declaredArity
            val writtenTypeParamNumber = o.arguments?.values?.size ?: 0
            if (writtenTypeParamNumber != exceptedTypeParamNumber) {
                o.error("Generic parameter mismatch, expected `$exceptedTypeParamNumber` but got `$writtenTypeParamNumber`")
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
                        RemoveElement(o.type.value, o),
                    )
                }
            }

            // 禁止类型校验
            if (qualifiedName?.startsWith("org.babyfish.jimmer.") == true) {
                o.error(
                    "Types under `org.babyfish.jimmer` are not allowed",
                    RemoveElement(o.type.value, o),
                )
            }
        }

        /**
         * 为属性上色
         */
        override fun visitPositiveProp(o: DTOPositiveProp) {
            val dto = o.parentOfType<DTODto>() ?: return

            // 当前属性为方法
            if (o.arg != null) {
                visitFunction(dto, o, o.name.value)
            }
            // 当前属性为非方法属性
            if (o.arg == null) {
                visitProp(dto, o, o.name.value)
            }

            o.optional?.let { checkOptional(dto, o, it) }
        }

        private fun visitFunction(dto: DTODto, prop: DTOPositiveProp, functionName: String) {
            // 方法存在性校验
            val function = Function.entries.find { it.expression == functionName }

            if (function == null) {
                prop.name.error("Unknown function `$functionName`")
                return
            } else {
                prop.name.style(DTOSyntaxHighlighter.FUNCTION)
            }

            // fold 校验
            if (function == Function.Fold) {
                if (prop.haveParent<DTOAliasGroup>()) {
                    prop.name.error("`fold` cannot be used inside alias group", RemoveElement("fold", prop))
                }
                // 方法参数不可为空校验
                val arg = prop.arg ?: return
                if (arg.isEmpty) {
                    arg.error("Function arg list cannot be empty")
                    return
                }
                // fold 没有后续校验需求
                return
            }

            // 方法参数不可为空校验
            val arg = prop.arg ?: return
            if (arg.isEmpty) {
                arg.error("Function arg list cannot be empty")
                return
            }

            // Spec 方法校验
            if (functionName == "id" && dto modifiedBy Modifier.Specification) {
                prop.name.error(
                    "`id` is forbidden by specification, replace it to `associatedIdEq`",
                    ReplaceName(prop.name, "associatedIdEq", Project::createPropName),
                )
            }

            if (function.whetherSpec && dto notModifiedBy Modifier.Specification) {
                prop.name.error("Cannot call the function `$functionName` because the current dto type is not specification")
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
                        RemoveElement(it.text, it),
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
                if (arg.values.size > 1 && prop.alias == null) {
                    prop.error("An alias must be specified because `$functionName` has multiple arguments")
                }
            }

            // 方法体存在性校验
            if (function.whetherBody) {
                if (prop.body == null) {
                    prop.name.error("The function '$functionName' requires a body", AddDtoBody(prop, functionName))
                }
            } else {
                prop.body?.let {
                    it.error("The function $functionName cannot have a body", RemoveElement("'$functionName' body", it))
                }
            }

            // id方法参数为list时别名校验
            if (function == Function.Id) {
                val value = arg.values[0]
                if (value.property?.isList == true) {
                    if (prop.alias == null) {
                        prop.error("An alias must be specified because the property `${value.text}` is a list association")
                    }
                }
            }

            // flat方法使用集合参数的校验
            if (function == Function.Flat) {
                val value = arg.values[0]
                if (dto notModifiedBy Modifier.Specification) {
                    if (value.property?.isList == true) {
                        prop.error("`flat` can only handle collection associations in specific modified dto")
                    }
                }
            }

            // like方法校验
            val flag = prop.flag
            if (flag != null) {
                if (function != Function.Like) {
                    prop.name.error("`/` can only be used to decorate the function `like`", RemoveElement(flag.text, flag))
                }

                val insensitive = flag.insensitive
                if (insensitive != null && insensitive.text != "i") {
                    insensitive.error(
                        "Illegal function option identifier `${insensitive.text}`, it can only be `i`",
                        ReplaceName(insensitive, "i", newElement = { insensitive.project.createInsensitive() }),
                    )
                }
            }

            // 属性 '!'
            val exclamation = prop.required
            if (exclamation != null) {
                val flat = Function.Flat.expression
                if (functionName == flat) {
                    exclamation.error("Illegal symbol '!', it's cannot decorate '$flat'")
                }
            }

            // 属性 '*'
            val recursive = prop.recursive
            if (recursive != null) {
                val value = arg.values.firstOrNull() ?: return
                recursive.error(
                    "Illegal symbol '*', the property '${value.text}' with function invocation '$functionName' cannot be recursive",
                    RemoveElement("*", recursive),
                )
            }
        }

        private fun visitProp(dto: DTODto, prop: DTOPositiveProp, propName: String) {
            val availableProperties = prop.containingLClass?.allProperties ?: return

            // 属性是否存在
            val property = availableProperties.find { it.name == propName } ?: let {
                prop.name.error("`$propName` does not exist")
                return
            }

            // 关联属性需要指定body
            if (property.isEntityAssociation && prop.body == null && prop.recursive == null) {
                prop.name.error("`$propName` must have child body")
            }

            // 枚举体
            val body = prop.body
            val enumBody = body?.enumBody
            if (enumBody != null && property.type !is LProperty.Type.Enum) {
                body.error(
                    "Enum body cannot be specified for non-enum property '$propName'",
                    RemoveElement("enum body", body),
                )
            }

            // as组中不允许直接子级使用as别名
            val alias = prop.alias
            if (alias != null && prop.parent.elementType == DTOLanguage.rule[DTOParser.RULE_aliasGroupBody]) {
                prop.error(
                    "Alias definition for direct children is prohibited in `alias-group`",
                    RemoveElement("Alias `${alias.text}` for $propName", alias, DTOLexer.As),
                )
            }

            // 属性 '*'
            val star = prop.recursive
            if (star != null) {
                if (!property.isRecursive) {
                    star.error(
                        "Illegal symbol '*', the property '$propName' is not recursive",
                        RemoveElement("*", star),
                    )
                } else {
                    if (body != null) {
                        star.error(
                            "The child body of recursive property '${propName}' cannot be specified",
                            RemoveElement("*", star),
                            RemoveElement("child body", body),
                        )
                    }

                    if (dto modifiedBy Modifier.Specification) {
                        star.error(
                            "Illegal symbol '*', recursive property cannot be declared in specification type",
                            RemoveElement("*", star),
                        )
                    }
                }
            }

            // 属性 '!'
            val exclamation = prop.required
            if (exclamation != null) {
                val specification = Modifier.Specification
                val input = Modifier.Input
                val unsafe = Modifier.Unsafe

                if (property.isId) {
                    if (dto notModifiedBy specification && dto notModifiedBy input) {
                        exclamation.error(
                            "'!' on id property requires 'input' or 'specification' dto",
                            RemoveElement("!", exclamation),
                        )
                    }
                } else {
                    if (dto notModifiedBy specification && dto notModifiedBy unsafe) {
                        exclamation.error(
                            "'!' on non-id property requires 'unsafe' or 'specification' dto",
                            RemoveElement("!", exclamation),
                        )
                        return
                    }

                    val flat = prop.parent<DTOPositiveProp>(false) { name.value == Function.Flat.expression && baseProperty?.nullable == true }
                    if (!property.nullable && flat == null && dto notModifiedBy Modifier.Specification) {
                        // TODO 上游调整为非异常后调整为 Inspection
                        exclamation.error(
                            "'!' is not allowed, the property is already non-null",
                            RemoveElement("!", exclamation),
                        )
                    }
                }
            }
        }

        // TODO 上游调整为非异常后调整为 Inspection
        fun checkOptional(dto: DTODto, prop: DTOPositiveProp, optional: PsiElement) {
            if (dto modifiedBy Modifier.Specification) {
                optional.error(
                    "'?' is not allowed, all properties of specification are already nullable",
                    RemoveElement("?", optional),
                )
                return
            }

            val propName = prop.name.value
            val flatFunctionName = Function.Flat.expression
            val propArg = prop.arg

            if (propArg != null && propName == flatFunctionName) {
                optional.error(
                    "'?' is not allowed for the function 'flat'",
                    RemoveElement("?", optional),
                )
                return
            }

            if ((propArg == null && prop.property?.nullable == true) || (propArg != null && propArg.values.firstOrNull()?.property?.nullable == true)) {
                val basePropName = if (propArg == null) propName else propArg.values.first().text
                optional.error(
                    "'?' is not allowed, the property '$basePropName' is already nullable",
                    RemoveElement("?", optional),
                )
                return
            }

            val parentFlat = prop.parent<DTOPositiveProp>(false) { name.value == flatFunctionName && baseProperty?.nullable == true }
            if (parentFlat != null) {
                val flatArg = parentFlat.baseProperty!!.name
                optional.error(
                    "'?' is not allowed, the enclosing '$flatFunctionName($flatArg)' is already nullable",
                    RemoveElement("?", optional),
                )
                return
            }
        }

        /**
         * 为属性配置上色
         */
        override fun visitPropConfig(o: DTOPropConfig) {
            val configName = o.name.text
            o.name.style(DTOSyntaxHighlighter.PROP_CONFIG)

            val prop = o.parent<DTOPositiveProp>() ?: return
            if (prop.arg != null) {
                o.name.error("Prop config '$configName' cannot be applied to the function '${prop.name.value}'")
                return
            }

            val dto = o.parent<DTODto>() ?: return
            if (dto modifiedBy Modifier.Specification || dto modifiedBy Modifier.Input) {
                o.error("Configuration can only be applied to output DTO")
            }

            val availableNames = PropConfigName.availableNames
            if (configName !in availableNames) {
                o.name.error(
                    "Incorrect prop-config name '$configName'",
                    ChangeReferenceFix(o.name, availableNames)
                )
                return
            }

            val conflicts = PropConfigName.exclusive[configName].orEmpty()
            prop.configs
                .takeWhile { it !== o }
                .firstOrNull { it.name.text in conflicts }
                ?.let {
                    o.name.error("Cannot specify '${o.name.text}' when '${it.name.text}' exists")
                }

            val property = prop.property ?: return
            o.qualifiedName?.validatePropPath()

            when (configName) {
                PropConfigName.Where.text -> {
                    if (!property.isEntityAssociation) {
                        o.name.error("Cannot specify '!where' when the property is not association")
                    }

                    if (property.isReference && !property.nullable) {
                        o.name.error("Cannot specify '!where' when the property is non-null reference")
                    }

                    if (o.whereArgs == null) {
                        o.name.error("Missing predicate in '!where'")
                    }
                }

                PropConfigName.OrderBy.text -> {
                    if (!property.isEntityAssociation || !property.isList) {
                        o.name.error("Cannot specify '!orderBy' when the property is not associated list")
                    }

                    val orderByArgs = o.orderByArgs
                    if ((orderByArgs == null || orderByArgs.orderItems.isEmpty()) && o.qualifiedName == null) {
                        o.name.error("Missing order items in '!orderBy'")
                    }
                }

                PropConfigName.Filter.text -> {
                    if (!property.isEntityAssociation || !property.isList) {
                        o.name.error("Cannot specify '!filter' when the property is not associated list")
                    }

                    val qualifiedName = o.qualifiedName
                    if (qualifiedName == null) {
                        o.name.error("Missing filter class in '!filter'")
                    } else {
                        val target = qualifiedName.target
                        if (target is Resolution.Target.Type) {
                            val targetEntity = property.targetClass?.dependencyItem ?: return
                            val filterEntity = compiling(prop) { filterEntity(target.type) } ?: return

                            if (!targetEntity.isEquivalentTo(filterEntity)) {
                                val targetEntityName = process(targetEntity) { classQualifiedName() } ?: return
                                val filterEntityName = process(filterEntity) { classQualifiedName() } ?: return
                                qualifiedName.error(
                                    "The filter class '${qualifiedName.text}' is illegal, " +
                                            "it specifies the entity type '$filterEntityName', " +
                                            "which is not the target entity type '$targetEntityName' of property '${property.name}'"
                                )
                            }
                        }
                    }
                }

                PropConfigName.Recursion.text -> {
                    if (!prop.isRecursive || !property.isRecursive) {
                        o.name.error("'!recursion' can only be applied for recursive property")
                    }

                    val qualifiedName = o.qualifiedName
                    if (qualifiedName == null) {
                        o.name.error("Missing recursion strategy class in '!recursion'")
                    } else {
                        val target = qualifiedName.target
                        if (target is Resolution.Target.Type) {
                            val targetEntity = property.targetClass?.dependencyItem ?: return
                            val strategyEntity = process(target.type) { typeArgumentFor("org.babyfish.jimmer.sql.fetcher.RecursionStrategy") }
                            strategyEntity?.takeIf { it !is PsiTypeParameter } ?: return

                            if (!targetEntity.isEquivalentTo(strategyEntity)) {
                                val targetEntityName = process(targetEntity) { classQualifiedName() } ?: return
                                val strategyName = process(strategyEntity) { classQualifiedName() } ?: return
                                qualifiedName.error(
                                    "The recursion class '${qualifiedName.text}' is illegal, " +
                                            "it specifies the entity type '$strategyName', " +
                                            "which is not the target entity type '$targetEntityName' of property '${property.name}'"
                                )
                            }
                        }
                    }
                }

                PropConfigName.FetchType.text -> {
                    if (!property.isEntityAssociation || property.isList) {
                        o.name.error("Cannot specify '!fetchType' when the property is not associated reference")
                    }

                    val fetchType = o.qualifiedName
                    if (fetchType != null) {
                        val fetchTypeValue = fetchType.value
                        val referenceFetchType = o.psiClass(Constant.REFERENCE_FETCH_TYPE) ?: return
                        val availableTypes = referenceFetchType.fields
                            .filterIsInstance<PsiEnumConstant>()
                            .map(PsiEnumConstant::getName)
                            .filter { it != "AUTO" }
                        val target = fetchType.target
                        if (target is Resolution.Target.EnumConst && fetchTypeValue in availableTypes) {
                            fetchType.style(DTOSyntaxHighlighter.VALUE)
                        } else {
                            fetchType.error(
                                "Incorrect fetchType '$fetchTypeValue'",
                                ChangeReferenceFix(fetchType, availableTypes),
                            )
                        }
                    } else {
                        o.name.error("Missing fetch type in '!fetchType'")
                    }
                }

                PropConfigName.Limit.text -> {
                    if (!property.isEntityAssociation || !property.isList) {
                        o.name.error("Cannot specify '!limit' when the property is not associated list")
                    }

                    val intPair = o.intPair
                    if (intPair != null) {
                        val limit = intPair.first
                        when (val limitValue = limit.text.toIntOrNull()) {
                            null -> limit.error("The limit is out of range")
                            else -> if (limitValue < 1) limit.error("The limit cannot be less than 1")
                        }

                        val offset = intPair.second
                        if (offset != null) {
                            when (val offsetValue = offset.text.toIntOrNull()) {
                                null -> offset.error("The offset is out of range")
                                else -> if (offsetValue < 0) offset.error("The offset cannot be less than 0")
                            }
                        }
                    } else {
                        o.name.error("Missing limit in '!limit'")
                    }
                }

                PropConfigName.Batch.text -> {
                    if (!property.isEntityAssociation || !property.isList) {
                        o.name.error("Cannot specify '!batch' when the property is not associated list")
                    }

                    val intPair = o.intPair
                    if (intPair != null) {
                        val batch = intPair.first
                        when (val batchValue = batch.text.toIntOrNull()) {
                            null -> batch.error("The batch is out of range")
                            else -> if (batchValue < 1) batch.error("The batch cannot be less than 1")
                        }

                        intPair.second?.error("'!batch' accepts only one numeric value")
                    } else {
                        o.name.error("Missing batch size in '!batch'")
                    }
                }

                PropConfigName.Depth.text -> {
                    if (!prop.isRecursive || !property.isRecursive) {
                        o.name.error("'!depth' can only be applied for recursive property")
                    }

                    val intPair = o.intPair
                    if (intPair != null) {
                        val depth = intPair.first
                        when (val depthValue = depth.text.toIntOrNull()) {
                            null -> depth.error("The limit is out of range")
                            else -> if (depthValue < 0) depth.error("The depth cannot be less than 0")
                        }

                        intPair.second?.error("!depth accepts only one numeric value")
                    } else {
                        o.name.error("Missing depth in '!depth'")
                    }
                }
            }
        }

        /**
         * !where 条件校验
         */
        override fun visitCompare(o: DTOCompare) {
            if (o.symbol == null && o.value == null) {
                o.prop.error("Missing comparison operator and value after '${o.prop.text}'")
            }

            val property = o.prop.validatePropPath() ?: return
            val simpleType = property.simplePropType
            if (simpleType == null) {
                val lastPart = o.prop.parts.last()
                lastPart.error("Property '${property.name}' has type '${property.type.presentation}', which cannot be used as an operand in '!where'; expected boolean, numeric, or string")
                return
            }
            val symbol = o.symbol ?: return
            val required = symbol.kind.requires
            if (required != null && simpleType.family != required) {
                symbol.error("The operator '${symbol.text}' is not allowed here because the left operand is not ${required.presentation}")
            }

            val value = o.value ?: run {
                symbol.error("Missing value after comparison operator '${symbol.text}'")
                return
            }
            if (value.kind.accepted != simpleType.family) {
                value.error("Illegal ${value.kind.literalName} literal, the left operand is ${simpleType.family.presentation}")
            } else if (!simpleType.fits(value.text)) {
                value.error("Illegal ${value.kind.literalName} literal '${value.text}', it is out of the range of $simpleType")
            }
        }

        /**
         * 空表达式校验
         */
        override fun visitNullity(o: DTONullity) {
            o.prop.validatePropPath()

            if (o.`null` == null) {
                o.`is`.error("Missing 'null' after 'is'")
            }
        }

        /**
         * 排序属性配置校验
         */
        override fun visitOrderItem(o: DTOOrderItem) {
            o.prop.validatePropPath()
        }

        /**
         * 排序方向校验
         */
        override fun visitOrderDirection(o: DTOOrderDirection) {
            val identifier = o.identifier
            identifier?.error(
                "The order mode is neither 'asc' nor 'desc'",
                ReplaceName(identifier, "asc", Project::createOrderDirection),
                ReplaceName(identifier, "desc", Project::createOrderDirection),
            )
        }

        /**
         * 属性路径校验
         */
        fun DTOQualifiedName.validatePropPath(): LProperty? {
            val lastIndex = parts.lastIndex
            var resolved: LProperty? = null

            for ((index, part) in parts.withIndex()) {
                val target = part.target as? Resolution.Target.Property ?: return null
                val property = target.property
                val propertyName = property.name
                resolved = property

                if (property.isEntityAssociation && property.isReference && index == lastIndex) {
                    part.error(
                        "Association '$propertyName' cannot be used here, use its id view '${propertyName}Id'",
                        ReplaceName(part, "${property.name}Id", Project::createQualifiedNamePart),
                    )
                    return null
                }

                if (property.isEntityAssociation && property.isReference && index < lastIndex) {
                    val idPropName = property.targetClass?.idProperty?.name
                    if (idPropName != null && parts[index + 1].part == idPropName) {
                        val old = "$propertyName.$idPropName"
                        val new = "${propertyName}Id"
                        part.error(
                            "The id of '$propertyName' should be accessed by id view '$new', not by '$old'",
                            ReplaceIdAccessorToView(this, index, old, new),
                        )
                        return null
                    }
                }

                if (property.isEntityAssociation && !property.isReference) {
                    return null
                }

                if (!property.isEntityAssociation && !property.isEmbedded && index < lastIndex) {
                    part.error("Cannot access members of '$propertyName', only embedded properties can be traversed")
                    return null
                }
            }

            return resolved
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
            val prop = o.parent<DTOPositiveProp>() ?: return
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
            val enumBody = o.parent<DTOEnumBody>() ?: return
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
            val fixerBuilder = holder.newAnnotation(HighlightSeverity.ERROR, message)
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
