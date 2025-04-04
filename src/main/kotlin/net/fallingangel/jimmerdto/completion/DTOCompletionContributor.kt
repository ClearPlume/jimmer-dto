package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import com.intellij.util.FileContentUtilCore
import net.fallingangel.jimmerdto.DTOLanguage.preludes
import net.fallingangel.jimmerdto.DTOLanguage.rule
import net.fallingangel.jimmerdto.DTOLanguage.token
import net.fallingangel.jimmerdto.completion.pattern.lsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOParser.*
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.*
import org.babyfish.jimmer.sql.Embeddable
import net.fallingangel.jimmerdto.psi.DTOParser.Modifier as ParserModifier
import net.fallingangel.jimmerdto.psi.DTOParser.PropConfigName as ParserPropConfig

class DTOCompletionContributor : CompletionContributor() {
    private val identifier = lsiElement(token[Identifier])
    private val error = lsiElement(TokenType.ERROR_ELEMENT)

    init {
        // 用户属性类型提示
        completeUserPropType()

        // 用户属性类型中的泛型提示
        completeUserPropGenericType()

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

        // 注解提示
        completeAnnotation()

        // 注解参数提示
        completeAnnotationParam()

        // 注解参数值提示
        completeAnnotationParamValue()

        // Class关键字提示
        completeClassKeyword()

        // Implements关键字提示
        completeImplementsKeyword()

        // 属性配置提示
        completePropConfig()

        // 属性配置参数提示
        completePropConfigArg()
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        context.dummyIdentifier = DUMMY_IDENTIFIER_TRIMMED
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        complete(
            { parameters, result ->
                val text = parameters.position.text.substringBefore(DUMMY_IDENTIFIER_TRIMMED)
                result.addAllElements(findUserPropType(text, parameters.originalFile as DTOFile))
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(3, DTOTypeRef::class.java)
                    .withSuperParent(4, DTOUserProp::class.java),
        )
    }

    /**
     * 用户属性类型中的泛型提示
     */
    private fun completeUserPropGenericType() {
        complete(
            { parameters, result ->
                val text = parameters.position.text.substringBefore(DUMMY_IDENTIFIER_TRIMMED)
                result.addAllElements(findUserPropType(text, parameters.originalFile as DTOFile, true))
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
                val prop = parameters.position.parent.parent<DTOPositiveProp>()
                result.addAllElements(prop.functions().lookUp())
                result.addAllElements(prop.allSiblings().lookUp())
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
                val propName = parameters.position.parent<DTOPropName>()
                result.addAllElements(propName.parent<DTONegativeProp>().allSiblings().lookUp())
            },
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(2, DTONegativeProp::class.java),
        )
    }

    /**
     * 宏提示
     */
    private fun completeMacro() {
        complete(
            { _, result ->
                result.addAllElements(listOf("allScalars", "allReferences").lookUp())
            },
            identifier.withParent(DTOMacroName::class.java),
        )
        complete(
            { parameters, result ->
                val macroArgs = parameters.position.parent.parent.parent<DTOMacro>()
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
                val prop = parameters.position.parent.parent.parent<DTOEnumBody>()
                result.addAllElements(prop.values.lookUp())
            },
            identifier.withParent(DTOEnumMappingConstant::class.java)
                    .withSuperParent(3, lsiElement(DTOEnumBody::class.java))
        )
    }

    /**
     * Dto修饰符提示
     */
    private fun completeDtoModifier() {
        complete(
            { parameters, result ->
                val dto = parameters.position.parent.parent<DTODto>()
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
                    val prop = position.parent.parent<DTOPositiveProp>()
                    val propPath = prop.propPath()
                    val proceedPropPath = propPath.dropLast(1) + propPath.last().replace(DUMMY_IDENTIFIER_TRIMMED, "")
                    val nullable = prop.file.clazz.findPropertyOrNull(proceedPropPath)?.nullable ?: return@complete
                    if (nullable) {
                        result.addAllElements(
                            Modifier.values()
                                    .filter { it.level == Modifier.Level.Both }
                                    .map {
                                        val modifier = it.name.lowercase()
                                        LookupInfo(modifier, "$modifier ")
                                    }
                                    .lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
                        )
                    }
                }
            },
            and(
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(2, DTOPositiveProp::class.java),
                identifier.withParent(
                    not(
                        lsiElement(DTOPropName::class.java)
                                .afterSibling(lsiElement(token[ParserModifier])),
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
                val propArgs = parameters.position.parent.parent<DTOPropArg>()
                result.addAllElements(propArgs.args.lookUp())
            },
            identifier.withParent(DTOValue::class.java)
                    .withSuperParent(2, lsiElement(DTOPropArg::class.java)),
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
                        lsiElement().withFirstNonWhitespaceChild(
                            lsiElement(DTODto::class.java)
                                    .withChild(lsiElement(DTODtoName::class.java)),
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
                            lsiElement().withFirstNonWhitespaceChild(
                                lsiElement(DTODto::class.java)
                                        .withChild(
                                            lsiElement(DTODtoName::class.java)
                                                    .withText(DUMMY_IDENTIFIER_TRIMMED),
                                        ),
                            ),
                        ),
                identifier
                        .andNot(identifier.withParent(error))
                        .withSuperParent(
                            2,
                            lsiElement(DTODto::class.java)
                                    .afterSibling(
                                        or(
                                            lsiElement(DTOExportStatement::class.java),
                                            lsiElement(DTOImportStatement::class.java)
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
        completePackage(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(3, DTOExportStatement::class.java),
            Project::allEntities,
            needImport = false,
        )
    }

    /**
     * Import包提示
     */
    private fun completeImportPackage() {
        completePackage(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(3, DTOImportStatement::class.java),
            Project::allClasses
        )
    }

    /**
     * 注解提示
     */
    private fun completeAnnotation() {
        completePackage(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(3, DTOAnnotation::class.java)
                    .andNot(identifier.withSuperParent(4, DTOAnnotationSingleValue::class.java)),
            Project::allAnnotations,
            true,
        )
    }

    /**
     * 包和类提示
     *
     * @param classAvailableBeforeFirstDot 类提示是否在全限定类名中的第一段可用
     */
    private inline fun completePackage(
        place: ElementPattern<PsiElement>,
        crossinline classes: Project.(String?) -> List<PsiClass>,
        classAvailableBeforeFirstDot: Boolean = false,
        needImport: Boolean = true,
    ) {
        complete(
            { parameters, result ->
                val project = parameters.position.project
                val typedPackage = parameters.position.parent.siblings(forward = false, withSelf = false)
                        .filter { it.elementType == rule[RULE_qualifiedNamePart] }
                        .map(PsiElement::getText)
                        .toList()
                        .asReversed()
                val curPackage = typedPackage.joinToString(".")
                val curPackageClasses = if (typedPackage.isEmpty() && classAvailableBeforeFirstDot) {
                    project.classes(null).lookUp(needImport)
                } else {
                    project.classes(curPackage).lookUp(needImport)
                }

                val availablePackages = project.allPackages(curPackage)
                        .map {
                            LookupElementBuilder.create(it.name!!)
                                    .withIcon(AllIcons.Nodes.Package)
                        }

                result.addAllElements(curPackageClasses + availablePackages)
            },
            place,
        )
    }

    /**
     * 注解参数提示
     */
    private fun completeAnnotationParam() {
        complete(
            { parameters, result ->
                val (annotationClass, params) = when (val parent = parameters.position.parent.parent) {
                    is DTOAnnotation -> parent.qualifiedName.clazz to parent.params.map { it.name.text }
                    is DTONestAnnotation -> parent.qualifiedName.clazz to parent.params.map { it.name.text }
                    else -> when (val anno = parameters.position.parent.parent.parent.parent.parent) {
                        is DTOAnnotation -> anno.qualifiedName.clazz to anno.params.map { it.name.text }
                        is DTONestAnnotation -> anno.qualifiedName.clazz to anno.params.map { it.name.text }
                        else -> null to emptyList()
                    }
                }
                annotationClass ?: return@complete
                val project = annotationClass.project
                val stringType = project.stringType
                val elements = annotationClass.methods
                        .filter { it.name !in params }
                        .map {
                            val paramType = it.returnType
                            val default = when {
                                parameters.position.parent is DTOAnnotationParameter -> ""
                                paramType == stringType -> " = \"\""
                                paramType is PsiArrayType && paramType.componentType == stringType -> " = [\"\"]"
                                paramType == PsiType.CHAR -> " = ''"
                                paramType is PsiArrayType && paramType.componentType == PsiType.CHAR -> " = ['']"
                                else -> " = "
                            }
                            LookupElementBuilder.create(it.name + default)
                                    .withIcon(AllIcons.Nodes.Property)
                                    .withPresentableText(it.name)
                                    .withTailText(default, true)
                                    .withTypeText(paramType?.canonicalText, true)
                                    .withInsertHandler { context, element ->
                                        val offset = if (default in listOf(" = ", "")) {
                                            0
                                        } else {
                                            if (paramType is PsiArrayType) {
                                                2
                                            } else {
                                                1
                                            }
                                        }
                                        context.editor.caretModel.moveToOffset(context.tailOffset - offset)
                                    }
                        }
                result.addAllElements(elements)
            },
            or(
                identifier.withParent(DTOAnnotationParameter::class.java),
                // 还没有输入参数时的情况
                identifier.withSuperParent(3, DTOAnnotationSingleValue::class.java)
                        .andNot(identifier.withParent(PsiErrorElement::class.java)),
            ),
        )
    }

    /**
     * 注解参数值提示(目前只有注解类型的参数实现了提示)
     */
    private fun completeAnnotationParamValue() {
        complete(
            { parameters, result ->
                if (parameters.position.parent is DTOAnnotationParameter) {
                    return@complete
                }

                val tripe = parameters.position.parent.parent.parent

                // @anno(param = dummy)
                val (param, anno) = if (tripe is DTOAnnotationSingleValue) {
                    // SingleValue往上二级或者三级是Parameter
                    val upper = tripe.parent.parent
                    val param = upper as? DTOAnnotationParameter ?: upper.parent.parent as DTOAnnotationParameter
                    // Parameter父级是Annotation
                    param.name.text to param.parent as DTOAnnotation
                } else {
                    val nestAnno = tripe as DTONestAnnotation
                    val upper = nestAnno.parent.parent
                    // NestAnnotation往上二级
                    when (upper) {
                        is DTOAnnotationParameter -> upper.name.text to upper.parent as DTOAnnotation

                        is DTOAnnotationValue -> when {
                            // value父级为注解
                            upper.parent is DTOAnnotation -> "value" to upper.parent as DTOAnnotation

                            // value父级为数组
                            upper.parent is DTOAnnotationArrayValue -> if (upper.parent.parent.parent is DTOAnnotation) {
                                "value" to upper.parent.parent.parent as DTOAnnotation
                            } else {
                                val param = upper.parent.parent.parent as DTOAnnotationParameter
                                param.name.text to param.parent as DTOAnnotation
                            }

                            // value父级为param
                            else -> {
                                val param = upper.parent as DTOAnnotationParameter
                                param.name.text to param.parent as DTOAnnotation
                            }
                        }

                        else -> {
                            val param = upper.parent.parent as DTOAnnotationParameter
                            param.name.text to param.parent as DTOAnnotation
                        }
                    }
                }

                val annotationClass = anno.qualifiedName.clazz
                val paramType = annotationClass?.methods?.find { it.name == param }?.returnType?.extract ?: return@complete
                if (paramType is PsiClassType) {
                    val clazz = paramType.resolve()
                    if (clazz?.isAnnotationType == true) {
                        result.addAllElements(listOf(clazz).lookUp())
                    }
                }
            },
            or(
                // @Anno(param = <caret>)
                identifier.withSuperParent(3, DTOAnnotationSingleValue::class.java)
                        .withSuperParent(5, DTOAnnotationParameter::class.java)
                        .andNot(identifier.withParent(PsiErrorElement::class.java)),
                // @Anno(param = [<caret>])
                identifier.withSuperParent(3, DTOAnnotationSingleValue::class.java)
                        .withSuperParent(7, DTOAnnotationParameter::class.java)
                        .andNot(identifier.withParent(PsiErrorElement::class.java)),
                // @Anno(param = Ne<caret>st())
                identifier.withSuperParent(3, DTONestAnnotation::class.java)
                        .withSuperParent(6, DTOAnnotationParameter::class.java),
                // @Anno(param = [Ne<caret>st()])
                identifier.withSuperParent(3, DTONestAnnotation::class.java)
                        .withSuperParent(8, DTOAnnotationParameter::class.java),
                // @Anno(Ne<caret>st())
                identifier.withSuperParent(3, DTONestAnnotation::class.java)
                        .withSuperParent(5, DTOAnnotationValue::class.java),
                // @Anno([Ne<caret>st()])
                identifier.withSuperParent(3, DTONestAnnotation::class.java)
                        .withSuperParent(7, DTOAnnotationValue::class.java),
            ),
        )
    }

    private fun completeClassKeyword() {
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("class").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            or(
                identifier.withParent(DTOEnumMappingConstant::class.java),
                identifier.withParent(error.afterSibling(lsiElement(rule[RULE_classSuffix]))),
                identifier.withParent(DTOFile::class.java)
                        .afterSibling(
                            or(
                                lsiElement(rule[RULE_classSuffix]),
                                lsiElement(token[Dot]),
                            ),
                        ),
            )
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
                identifier.withParent(error.afterSibling(lsiElement(DTODtoName::class.java))),
                /*
                 * dto {
                 *     prop DUMMY_IDENTIFIER_TRIMMED
                 * }
                 */
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(
                            2,
                            // prop DUMMY_IDENTIFIER_TRIMMED
                            lsiElement(DTOPositiveProp::class.java)
                                    .afterSibling(
                                        // prop(...) DUMMY_IDENTIFIER_TRIMMED
                                        lsiElement(DTOPositiveProp::class.java)
                                                .andNot(
                                                    lsiElement(DTOPositiveProp::class.java)
                                                            .withChild(lsiElement(DTOPropArg::class.java)),
                                                ),
                                    )
                                    .andNot(
                                        // prop DUMMY_IDENTIFIER_TRIMMED@Anno
                                        lsiElement(DTOPositiveProp::class.java)
                                                .withChild(
                                                    lsiElement(DTOPropBody::class.java)
                                                            .withChild(lsiElement(DTOAnnotation::class.java)),
                                                ),
                                    ),
                        ),
            ),
        )
    }

    /**
     * 属性配置提示
     */
    private fun completePropConfig() {
        complete(
            { parameters, result ->
                val prop = parameters.position.parent.parent<DTOPositiveProp>()
                val propPath = prop.propPath()
                val proceedPropPath = propPath.drop(1) + propPath.last().replace(DUMMY_IDENTIFIER_TRIMMED, "")

                val clazz = prop.file.clazz.findPropertyOrNull(proceedPropPath)?.actualType as? LClass<*> ?: return@complete
                val property = prop.file.clazz.findPropertyOrNull(proceedPropPath) ?: return@complete

                val haveFilter = prop.hasConfig(PropConfigName.Filter)
                val haveWhere = prop.hasConfig(PropConfigName.Where)
                val haveOrderBy = prop.hasConfig(PropConfigName.OrderBy)
                val haveDepth = prop.hasConfig(PropConfigName.Depth)
                val haveRecursion = prop.hasConfig(PropConfigName.Recursion)

                val isEntityAssociation = property.isEntityAssociation
                val propertyIsList = property.isList

                val availableConfigs = PropConfigName.values().toMutableList()

                if (haveFilter || !isEntityAssociation || property.isReference && !property.nullable) {
                    availableConfigs -= PropConfigName.Where
                }

                if (haveFilter || !isEntityAssociation || !propertyIsList) {
                    availableConfigs -= PropConfigName.OrderBy
                }

                if (haveWhere || haveOrderBy || !isEntityAssociation || !propertyIsList) {
                    availableConfigs -= PropConfigName.Filter
                }

                if (haveDepth || property.actualType != clazz) {
                    availableConfigs -= PropConfigName.Recursion
                }

                if (!isEntityAssociation || propertyIsList) {
                    availableConfigs -= PropConfigName.FetchType
                }

                if (!isEntityAssociation || !propertyIsList) {
                    availableConfigs -= PropConfigName.Limit
                }

                if (!propertyIsList) {
                    availableConfigs -= PropConfigName.Batch
                }

                if (haveRecursion || property.actualType != clazz) {
                    availableConfigs -= PropConfigName.Depth
                }

                result.addAllElements(
                    availableConfigs
                            .map { it.text.drop(1) }
                            .lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
                )
            },
            lsiElement(token[ParserPropConfig]),
        )
    }

    /**
     * 属性配置参数提示
     */
    private fun completePropConfigArg() {
        complete(
            { parameters, result ->
                val error = parameters.position.parentUnSure<PsiErrorElement>()
                val prop = if (error == null) {
                    val expression = parameters.position.parent.parent<DTOQualifiedName>()
                    when (expression.parent.parent) {
                        is DTOOrderByArgs -> expression.parent.parent.parent.parent<DTOPositiveProp>()
                        is DTOPositiveProp -> expression.parent.parent<DTOPositiveProp>()
                        else -> expression.parent.parent.parent.parent.parent<DTOPositiveProp>()
                    }
                } else {
                    error.parent.parent<DTOPositiveProp>()
                }

                val properties = prop.allSiblings(true)

                val scalars = properties
                        .filter { it.type is LType.ScalarType }
                        .map { it.name to it.presentableType }
                val associations = properties
                        .filter { it.isReference && it.isEntityAssociation }
                        .map { it.name to it.presentableType }
                val views = properties
                        .filter { it.isReference && it.isEntityAssociation }
                        .map { "${it.name}Id" to it.presentableType }
                val embeddable = properties
                        .filter { it.doesTypeHaveAnnotation(Embeddable::class) }
                        .map { it.name to it.presentableType }

                result.addAllElements(
                    (scalars + associations + views + embeddable)
                            .map { (name, type) ->
                                LookupElementBuilder.create(name).withTypeText(type, true)
                            }
                )
            },
            or(
                identifier.withSuperParent(
                    5,
                    lsiElement(DTOWhereArgs::class.java)
                            .afterSiblingSkipping(
                                lsiElement(token[LParen]),
                                or(
                                    lsiElement(token[ParserPropConfig]).withText("!where"),
                                    lsiElement(token[ParserPropConfig]).withText("!orderBy"),
                                ),
                            ),
                ),
                identifier.withSuperParent(
                    4,
                    lsiElement(DTOOrderByArgs::class.java)
                            .afterSiblingSkipping(
                                lsiElement(token[LParen]),
                                or(
                                    lsiElement(token[ParserPropConfig]).withText("!where"),
                                    lsiElement(token[ParserPropConfig]).withText("!orderBy"),
                                ),
                            ),
                ),
                identifier.withParent(error.afterSibling(lsiElement(DTOWhereArgs::class.java))),
                identifier.withSuperParent(
                    2,
                    lsiElement(DTOQualifiedName::class.java)
                            .afterSiblingSkipping(
                                lsiElement(token[LParen]),
                                or(
                                    lsiElement(token[ParserPropConfig]).withText("!where"),
                                    lsiElement(token[ParserPropConfig]).withText("!orderBy"),
                                ),
                            ),
                ),
            ),
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

    private fun findUserPropType(prefix: String, file: DTOFile, isGeneric: Boolean = false): List<LookupElement> {
        val cache = PsiShortNamesCache.getInstance(file.project)
        val classes = cache
                .allClassNames
                .filter { it.startsWith(prefix) }
                .flatMap { cache.getClassesByName(it, ProjectScope.getAllScope(file.project)).toList() }
                .toList()
                .lookUp()

        val genericModifiers = if (isGeneric) {
            listOf("out", "in").lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        } else {
            emptyList()
        }

        val preludes = preludes.lookUp()
        val imports = (file.imported.values + file.importedAlias.values.map { it.second })
                .filterNot { it.isAnnotationType }
                .lookUp()
        return preludes +
                genericModifiers +
                imports +
                classes
    }

    private fun bodyLookups(): List<LookupElement> {
        val macros = listOf(
            LookupInfo(
                "#allScalars",
                "#allScalars",
                "macro",
                "(Type+)"
            )
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
    private fun List<LProperty<*>>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.name)
                    .withTypeText(it.presentableType, true)
                    .customizer()
        }
    }

    @JvmName("lookupPsiClass")
    private fun List<PsiClass>.lookUp(needImport: Boolean = true, customizer: LookupElementBuilder.() -> LookupElement = { this }) = mapNotNull {
        val qualifiedName = it.qualifiedName ?: return@mapNotNull null
        val name = it.name!!
        LookupElementBuilder.create(qualifiedName, name)
                .withIcon(it.icon)
                .withTypeText("(${qualifiedName.substringBeforeLast('.')})", true)
                .withInsertHandler { context, _ ->
                    if (needImport) {
                        val file = context.file as DTOFile
                        val fileNode = file.findChild<PsiElement>("/dtoFile").node
                        val export = file.export
                        val imports = file.imports
                        val importedSameName = name in file.imported.keys || name in file.importedAlias.keys
                        val import = imports.find { i -> i.qualifiedName.value == qualifiedName }
                        val annotationName = file.findElementAt(context.startOffset)?.parent?.parentUnSure<DTOQualifiedName>()
                        annotationName ?: return@withInsertHandler

                        if (qualifiedName == annotationName.value) {
                            // 要导入的名称和当前已输入的名称相等
                            return@withInsertHandler
                        }

                        if (importedSameName) {
                            if (import == null) {
                                val newAnnotationName = context.project.createAnnotation(qualifiedName).qualifiedName
                                annotationName.parent.node.replaceChild(annotationName.node, newAnnotationName.node)
                            }
                        } else {
                            if (import == null) {
                                if (imports.isEmpty()) {
                                    if (export == null) {
                                        fileNode.addLeaf(TokenType.WHITE_SPACE, "import $qualifiedName\n\n", fileNode.firstChildNode)
                                    } else {
                                        fileNode.addLeaf(
                                            token[Import],
                                            "\n\nimport $qualifiedName",
                                            export.node.treeNext,
                                        )
                                    }
                                } else {
                                    fileNode.addLeaf(
                                        token[Import],
                                        "\nimport $qualifiedName",
                                        imports.last().node.treeNext,
                                    )
                                }
                            }
                        }
                        FileContentUtilCore.reparseFiles(file.virtualFile)
                    }
                }
                .customizer()
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
