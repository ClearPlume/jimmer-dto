package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.completion.pattern.dtoElement
import net.fallingangel.jimmerdto.core.DTOLanguage.rule
import net.fallingangel.jimmerdto.core.DTOLanguage.token
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.compiling
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerTypes
import net.fallingangel.jimmerdto.lsi.jimmer.isEntity
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.DTOParser.Identifier
import net.fallingangel.jimmerdto.psi.demand
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.inheritors
import net.fallingangel.jimmerdto.util.modifiedBy
import net.fallingangel.jimmerdto.util.parent
import net.fallingangel.jimmerdto.util.psiClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.psi.DTOParser.Modifier as ParserModifier
import net.fallingangel.jimmerdto.psi.DTOParser.PropConfigName as ParserPropConfig

class DTOCompletionContributor : CompletionContributor() {
    private val identifier = dtoElement(token[Identifier])
    private val error = dtoElement(TokenType.ERROR_ELEMENT)

    init {
        // 用户属性类型提示
        completeUserPropType()

        // 用户属性类型中的泛型提示
        completeGenericType()

        // 正属性提示
        completeProp()

        // 负属性提示
        completeNegativeProp()

        // 宏提示
        completeMacro()

        // 枚举提示
        completeEnum()

        // Dto修饰符提示
        completeDtoModifier()

        // 正属性修饰符提示
        completePositivePropModifier()

        // 方法参数提示
        completeFunctionParameter()

        // Export关键字提示
        completeExportKeyword()

        // Import关键字提示
        completeImportKeyword()

        // Export包提示
        completeExportPackage()

        // Import包提示
        completeImportPackage()

        // Import 分组提示
        completeImportGroup()

        // 注解提示
        completeAnnotation()

        // 注解参数提示
        completeAnnotationParam()

        // 注解参数值提示
        completeAnnotationParamValue()

        // 注解参数 Class 关键字提示
        completeAnnotationClassKeyword()

        // Implements关键字提示
        completeImplementsKeyword()

        // Implements 列表提示
        completeImplementsType()

        // 属性配置提示
        completePropConfig()

        // 属性配置参数提示
        completePropConfigArg()

        // 多态分支目标类型提示
        completeMorphismTarget()

        // 多态分支类定义 class 关键字提示
        completeMorphismClassKeyword()
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        context.dummyIdentifier = DUMMY_IDENTIFIER_TRIMMED
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        complete(
            ::completeQualifiedNamePart,
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .withSuperParent(3, DTOTypeRef::class.java)
                .withSuperParent(4, DTOUserProp::class.java),
        )
    }

    /**
     * 类型中的泛型提示
     */
    private fun completeGenericType() {
        complete(
            { parameters, result ->
                result.addAllElements(listOf("out", "in").lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) })
                completeQualifiedNamePart(parameters, result)
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .withSuperParent(3, DTOTypeRef::class.java)
                .withSuperParent(4, DTOGenericArgument::class.java),
        )
    }

    /**
     * 正属性提示
     */
    private fun completeProp() {
        complete(
            { parameters, result ->
                result.addAllElements(bodyLookups())
                val prop = parameters.position.parent<DTOPositiveProp>() ?: return@complete
                result.addAllElements(prop.functions().lookUp())
                result.addAllElements(prop.containingLClass?.allProperties?.lookUp() ?: emptyList())
            },
            identifier.withParent(DTOPropName::class.java)
                .withSuperParent(2, DTOPositiveProp::class.java),
        )
    }

    /**
     * 负属性名提示
     */
    private fun completeNegativeProp() {
        complete(
            { parameters, result ->
                val negativeProp = parameters.position.parent<DTONegativeProp>() ?: return@complete
                result.addAllElements(negativeProp.containingLClass?.allProperties?.lookUp() ?: emptyList())
            },
            identifier.withParent(DTOPropName::class.java)
                .withSuperParent(2, DTONegativeProp::class.java),
        )
    }

    /**
     * 宏提示
     * 
     * TODO 优化可选项获取逻辑
     */
    private fun completeMacro() {
        complete(
            { _, result ->
                result.addAllElements(listOf("allScalars", "allReferences", "exhaustive").lookUp())
            },
            identifier.withParent(DTOMacroName::class.java),
        )
        complete(
            { parameters, result ->
                val macroArgs = parameters.position.parent<DTOMacro>() ?: return@complete
                result.addAllElements(macroArgs.types.lookUp())
            },
            identifier.withParent(DTOMacroArg::class.java),
        )
    }

    /**
     * 枚举提示
     */
    private fun completeEnum() {
        complete(
            { parameters, result ->
                val prop = parameters.position.parent<DTOEnumBody>() ?: return@complete
                result.addAllElements(prop.values.lookUp())
            },
            identifier.withParent(DTOEnumMappingConstant::class.java)
                .withSuperParent(3, dtoElement(DTOEnumBody::class.java))
        )
    }

    /**
     * Dto修饰符提示
     */
    private fun completeDtoModifier() {
        complete(
            { parameters, result ->
                val dto = parameters.position.parent<DTODto>() ?: return@complete
                result.addAllElements(
                    dto.availableModifiers
                        .map { LookupInfo(it, "$it ") }
                        .lookUp {
                            PrioritizedLookupElement.withPriority(bold(), 100.0)
                        }
                )
            },
            identifier.withParent(DTODtoName::class.java)
                .withSuperParent(2, DTODto::class.java),
        )
    }

    /**
     * 正属性修饰符提示
     */
    private fun completePositivePropModifier() {
        complete(
            { parameters, result ->
                val position = parameters.position
                val dto = position.parentOfType<DTODto>() ?: return@complete

                if (dto modifiedBy Modifier.Input) {
                    // TODO 无条件提示、写错了交给 Annotator 事后报
                    result.addAllElements(
                        Modifier.entries
                            .filter { it.level == Modifier.Level.Both }
                            .map {
                                val modifier = it.name.lowercase()
                                LookupInfo(modifier, "$modifier ")
                            }
                            .lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
                    )
                }
            },
            and(
                identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(2, DTOPositiveProp::class.java),
                identifier.withParent(
                    not(
                        dtoElement(DTOPropName::class.java)
                            .afterSibling(dtoElement(token[ParserModifier])),
                    ),
                ),
            )
        )
    }

    /**
     * 提示方法参数
     */
    private fun completeFunctionParameter() {
        complete(
            { parameters, result ->
                val propArgs = parameters.position.parent<DTOPropArg>() ?: return@complete
                result.addAllElements(propArgs.args?.lookUp() ?: emptyList())
            },
            identifier.withParent(DTOValue::class.java)
                .withSuperParent(2, dtoElement(DTOPropArg::class.java)),
        )
    }

    /**
     * Export关键字提示
     */
    private fun completeExportKeyword() {
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("export").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            identifier
                .withSuperParent(
                    3,
                    dtoElement().withFirstNonWhitespaceChild(
                        dtoElement(DTODto::class.java)
                            .withChild(dtoElement(DTODtoName::class.java)),
                    ),
                ),
        )
    }

    /**
     * Import关键字提示
     */
    private fun completeImportKeyword() {
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("import").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            or(
                identifier
                    .withSuperParent(
                        3,
                        dtoElement().withFirstNonWhitespaceChild(
                            dtoElement(DTODto::class.java)
                                .withChild(
                                    dtoElement(DTODtoName::class.java)
                                        .withText(DUMMY_IDENTIFIER_TRIMMED),
                                ),
                        ),
                    ),
                identifier
                    .andNot(identifier.withParent(error))
                    .withSuperParent(
                        2,
                        dtoElement(DTODto::class.java)
                            .afterSibling(
                                or(
                                    dtoElement(DTOExportStatement::class.java),
                                    dtoElement(DTOImportStatement::class.java)
                                ),
                            ),
                    ),
            ),
        )
    }

    /**
     * Export包提示
     */
    private fun completeExportPackage() {
        complete(
            { parameters, result ->
                completeQualifiedNamePart(parameters, result) {
                    it is PsiPackage || (process(it) { isEntity() } ?: false)
                }
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .withSuperParent(3, DTOExportStatement::class.java),
        )
    }

    /**
     * Import包提示
     */
    private fun completeImportPackage() {
        complete(
            ::completeQualifiedNamePart,
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .withSuperParent(3, DTOImportStatement::class.java),
        )
    }

    /**
     * Import 分组提示
     */
    private fun completeImportGroup() {
        complete(
            { parameters, result ->
                val importGroup = parameters.position.parent<DTOGroupedImport>() ?: return@complete
                val space = importGroup.target?.spaceForMembers() ?: return@complete
                result.addAllElements(space.candidates().lookUp(false))
            },
            identifier.withParent(DTOImported::class.java)
                .inside(DTOImportStatement::class.java),
        )
    }

    /**
     * 注解提示
     */
    private fun completeAnnotation() {
        complete(
            { parameters, result ->
                completeQualifiedNamePart(parameters, result) {
                    process(it) { kind() == LKind.Annotation } ?: false
                }
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .withSuperParent(3, DTOAnnotation::class.java)
                .andNot(identifier.withSuperParent(4, DTOAnnotationSingleValue::class.java)),
        )
    }

    /**
     * 注解参数提示
     */
    private fun completeAnnotationParam() {
        complete(
            { parameters, result ->
                val annotation = parameters.position.parent<DTOAnnotationElement>() ?: return@complete
                val writtenParams = annotation.params.mapTo(mutableSetOf()) { it.name.text }
                val params = annotation.lAnnotation?.params ?: return@complete

                params
                    .filter { it.name !in writtenParams }
                    .forEach { param ->
                        result.addElement(
                            LookupElementBuilder.create(param.dependencyItem, param.name)
                                .withIcon(param.dependencyItem.getIcon(0))
                                .withTailText(" =")
                                .withInsertHandler { context, _ ->
                                    if (parameters.position.parent !is DTOAnnotationParameter) {
                                        context.document.insertString(context.tailOffset, " = ")
                                        context.editor.caretModel.moveToOffset(context.tailOffset)
                                    }
                                },
                        )
                    }
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .inside(
                    dtoElement(DTOAnnotationValue::class.java)
                        .withParent(dtoElement(rule[DTOParser.RULE_annotationArgs])),
                ),
        )
    }

    /**
     * 注解参数值提示
     */
    private fun completeAnnotationParamValue() {
        complete(
            { parameters, result ->
                completeQualifiedNamePart(parameters, result)

                val annotation = parameters.position.parent<DTOAnnotationElement>() ?: return@complete
                val param = annotation.paramAt(parameters.position) ?: return@complete
                when (val paramType = param.actualType) {
                    is ParamType.Scalar if (paramType.kind == ParamType.Scalar.Kind.Boolean) -> {
                        result.addAllElements(
                            listOf("true", "false").lookUp {
                                PrioritizedLookupElement.withPriority(bold(), 100.0)
                            }
                        )
                    }

                    else -> {}
                }
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .inside(
                    dtoElement(DTOAnnotationParameter::class.java)
                        .withParent(dtoElement(rule[DTOParser.RULE_annotationArgs])),
                ),
        )
    }

    /**
     * 注解参数 Class 关键字提示
     */
    private fun completeAnnotationClassKeyword() {
        complete(
            { parameters, result ->
                val annotation = parameters.position.parent<DTOAnnotationElement>() ?: return@complete
                val param = annotation.paramAt(parameters.position) ?: return@complete

                if (param.actualType is ParamType.Clazz) {
                    result.addAllElements(
                        listOf("class").lookUp {
                            PrioritizedLookupElement.withPriority(bold(), 100.0)
                        }
                    )
                }
            },
            identifier
                .withParent(
                    or(
                        dtoElement(rule[DTOParser.RULE_classSuffix]),
                        dtoElement(DTOQualifiedNamePart::class.java)
                            .afterSibling(dtoElement(token[DTOLexer.Dot])),
                    )
                )
                .inside(DTOAnnotation::class.java)
        )
    }

    /**
     * Implements关键字提示
     */
    private fun completeImplementsKeyword() {
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("implements").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            or(
                // dto DUMMY_IDENTIFIER_TRIMMED { ... }
                identifier.withParent(error.afterSibling(dtoElement(DTODtoName::class.java))),
                /*
                 * dto {
                 *     prop DUMMY_IDENTIFIER_TRIMMED
                 * }
                 */
                identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(
                        2,
                        // prop DUMMY_IDENTIFIER_TRIMMED
                        dtoElement(DTOPositiveProp::class.java)
                            .afterSibling(
                                // prop(...) DUMMY_IDENTIFIER_TRIMMED
                                dtoElement(DTOPositiveProp::class.java)
                                    .andNot(
                                        dtoElement(DTOPositiveProp::class.java)
                                            .withChild(dtoElement(DTOPropArg::class.java)),
                                    ),
                            )
                            .andNot(
                                // prop DUMMY_IDENTIFIER_TRIMMED@Anno
                                dtoElement(DTOPositiveProp::class.java)
                                    .withChild(
                                        dtoElement(DTOPropBody::class.java)
                                            .withChild(dtoElement(DTOAnnotation::class.java)),
                                    ),
                            ),
                    ),
            ),
        )
    }

    /**
     * Implements 列表提示
     */
    private fun completeImplementsType() {
        complete(
            ::completeQualifiedNamePart,
            identifier.withParent(DTOQualifiedNamePart::class.java)
                .inside(DTOImplements::class.java)
        )
    }

    /**
     * 属性配置提示
     */
    private fun completePropConfig() {
        complete(
            { parameters, result ->
                val prop = parameters.position.parent<DTOPositiveProp>() ?: return@complete
                val config = parameters.position.parent<DTOPropConfig>() ?: return@complete

                val property = prop.property ?: return@complete
                val availableConfigs = PropConfigName.entries.filter { it.violations(config, prop, property).isEmpty() }

                result.addAllElements(
                    availableConfigs
                        .map { it.text.drop(1) }
                        .lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
                )
            },
            dtoElement(token[ParserPropConfig]),
        )
    }

    /**
     * 属性配置参数提示
     */
    private fun completePropConfigArg() {
        complete(
            { parameters, result ->
                val propConfig = parameters.position.parent<DTOPropConfig>() ?: return@complete

                when (propConfig.name.text) {
                    in arrayOf(PropConfigName.Where.text, PropConfigName.OrderBy.text) -> {
                        val part = parameters.position.parent<DTOQualifiedNamePart>() ?: return@complete
                        val candidates = part.space?.candidates() ?: return@complete
                        result.addAllElements(candidates.lookUp(false))
                    }

                    PropConfigName.Recursion.text -> {
                        result.addAllElements(
                            JimmerTypes.RecursionStrategy.inheritors(element = propConfig)
                                .map { it.lookUp(it.demand(PsiClass::getName), true) }
                        )
                    }

                    PropConfigName.Filter.text -> {
                        val fieldFilterName = compiling(propConfig) { fieldFilterName() } ?: return@complete
                        result.addAllElements(
                            fieldFilterName.inheritors(element = propConfig)
                                .map { it.lookUp(it.demand(PsiClass::getName), true) }
                        )
                    }

                    PropConfigName.FetchType.text -> {
                        val referenceFetchType = JimmerTypes.ReferenceFetchType.psiClass(element = propConfig) ?: return@complete
                        val availableTypes = referenceFetchType.fields
                            .filterIsInstance<PsiEnumConstant>()
                            .filter { it.name != "AUTO" }

                        result.addAllElements(availableTypes.map { LookupElementBuilder.createWithIcon(it) })
                    }
                }
            },
            identifier.inside(DTOPropConfig::class.java),
        )
    }

    /**
     * 多态分支目标类型提示
     */
    private fun completeMorphismTarget() {
        complete(
            { parameters, result ->
                completeQualifiedNamePart(parameters, result) {
                    it is PsiPackage || (process(it) { isEntity() } ?: false)
                }
            },
            identifier.withParent(
                dtoElement(DTOQualifiedNamePart::class.java)
                    .withSuperParent(2, DTOTypeMorphism::class.java)
            ),
        )
    }

    /**
     * 多态分支类定义 class 关键字提示
     */
    private fun completeMorphismClassKeyword() {
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("class").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            identifier.withParent(dtoElement(DTOClassKeyword::class.java))
                .inside(dtoElement(DTOMorphism::class.java)),
        )
    }

    /**
     * 提示指定位置的内容
     *
     * @param place 元素位置表达式
     * @param provider 内容提示
     */
    private fun complete(provider: (CompletionParameters, CompletionResultSet) -> Unit, place: ElementPattern<PsiElement>) {
        extend(
            CompletionType.BASIC,
            place,
            object : CompletionProvider() {
                override fun completions(parameters: CompletionParameters, result: CompletionResultSet) {
                    provider(parameters, result)
                }
            }
        )
    }

    private fun completeQualifiedNamePart(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        filter: (PsiNamedElement) -> Boolean = { true },
    ) {
        val part = parameters.position.parent<DTOQualifiedNamePart>() ?: return
        when (val space = part.space) {
            null -> return

            is Resolution.Space.GlobalWithImports -> {
                val emitted = mutableSetOf<String>()
                for (candidate in space.candidates()) {
                    val target = candidate.target
                    val subject = (target as? Resolution.Target.Alias)?.target?.type ?: target.source
                    if (filter(subject)) {
                        result.addElement(candidate.lookUp(true))
                        emitted.add(candidate.name)
                    }
                }
                space.global.eachClass(parameters = parameters, matcher = result.prefixMatcher) {
                    val name = it.demand(PsiNamedElement::getName)
                    if (name !in emitted && filter(it)) {
                        result.addElement(it.lookUp(name, true))
                    }
                }
                result.restartCompletionOnAnyPrefixChange()
            }

            is Resolution.Space.Subtypes -> {
                result.addAllElements(
                    listOf("default").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
                result.addAllElements(
                    space.candidates()
                        .filter { filter(it.target.source) }
                        .lookUp(true)
                )
                val entity = space.lClass.dependencyItem
                result.addElement(entity.lookUp(entity.demand(PsiNamedElement::getName), false))
                result.addAllElements(
                    space.global
                        .candidates()
                        .filter {
                            val subject = (it.target as? Resolution.Target.Alias)?.target?.type ?: it.target.source
                            filter(subject)
                        }
                        .lookUp(false)
                )
            }

            else -> {
                result.addAllElements(
                    space.candidates()
                        .filter { filter(it.target.source) }
                        .lookUp(false)
                )
            }
        }
    }

    private fun bodyLookups(): List<LookupElement> {
        val macros = listOf(
            LookupInfo(
                "#allScalars",
                "#allScalars",
                "macro",
                "(Type+)"
            ),
            LookupInfo(
                "#allReferences",
                "#allReferences",
                "macro",
                "(Type+)"
            ),
        ).lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        val aliasGroup = listOf(
            LookupInfo(
                "as",
                "as() {}",
                "alias-group",
                "(<original> -> <replacement>) { ... }",
                -4
            )
        ).lookUp { PrioritizedLookupElement.withPriority(bold(), 90.0) }
        return macros + aliasGroup
    }

    @JvmName("lookupString")
    private fun List<String>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it).customizer()
        }
    }

    @JvmName("lookupProperty")
    private fun List<LProperty>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.name)
                .withTypeText(it.presentableType, true)
                .customizer()
        }
    }

    @JvmName("lookupInfo")
    private fun List<LookupInfo>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.insertion)
                .withPresentableText(it.presentation)
                .withTailText(it.tail, true)
                .withTypeText(it.type, true)
                .withInsertHandler { context, _ ->
                    if (it.caretOffset != 0) {
                        context.editor.caretModel.moveToOffset(context.tailOffset + it.caretOffset)
                    }
                }
                .customizer()
        }
    }
}
