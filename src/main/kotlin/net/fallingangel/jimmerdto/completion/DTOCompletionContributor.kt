package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.TokenType
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass

class DTOCompletionContributor : CompletionContributor() {
    private val identifier = psiElement(DTOTypes.IDENTIFIER)
    private val whitespace = psiElement(TokenType.WHITE_SPACE)
    private val error = psiElement(TokenType.ERROR_ELEMENT)

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

        // AliasGroup方法体提示
        completeAsBody()

        // 方法参数提示
        completeFunctionParameter()

        // Flat方法体提示
        completeFlatBody()

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

        // 嵌套注解提示
        completeNestAnnotation()

        // 注解参数提示
        completeAnnotationParam()

        // 嵌套注解参数提示
        completeNestAnnotationParam()

        // Class关键字提示
        completeClassKeyword()

        // Implements关键字提示
        completeImplementsKeyword()

        // 属性配置提示
        completePropConfig()
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val parent = element.parent
        when {
            parent is DTOEnumInstanceValue -> context.dummyIdentifier = ""

            parent is DTOEnumInstanceMapping
                    && element.prevSibling.elementType == DTOTypes.COLON -> context.dummyIdentifier = ""

            parent.parent is DTOPropArgs -> context.dummyIdentifier = ""

            parent is DTOAnnotationArrayValue -> context.dummyIdentifier += "()"

            parent is DTOAnnotationName && parent.parent is DTOAnnotationConstructor && parent.parent.parent is DTOAnnotation -> {
                context.dummyIdentifier = DUMMY_IDENTIFIER_TRIMMED
            }

            parent is DTOWhereArgs -> context.dummyIdentifier += " > 0"

            parent !is DTOWhereArgs && psiElement().inside(DTOWhereArgs::class.java).accepts(parent) -> {
                context.dummyIdentifier = DUMMY_IDENTIFIER_TRIMMED
            }

            parent is DTOPropConfig -> context.dummyIdentifier = ""

            else -> return
        }
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        complete(
            { parameters, result ->
                result.addAllElements(findUserPropType(parameters.originalFile as DTOFile))
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(2, psiElement(DTOQualifiedName::class.java))
                    .withSuperParent(3, DTOTypeDef::class.java)
                    .withSuperParent(4, DTOUserProp::class.java)
        )
    }

    /**
     * 用户属性类型中的泛型提示
     */
    private fun completeUserPropGenericType() {
        complete(
            { parameters, result ->
                result.addAllElements(findUserPropType(parameters.originalFile as DTOFile, true))
            },
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(2, psiElement(DTOQualifiedName::class.java))
                    .withSuperParent(3, DTOTypeDef::class.java)
                    .withSuperParent(4, DTOGenericArg::class.java)
        )
    }

    /**
     * 正属性提示
     */
    private fun completeProp() {
        complete(
            { parameters, result ->
                result.addAllElements(bodyLookups())
                val propName = parameters.parent<DTOPropName>()
                result.addAllElements(
                    propName[StructureType.PropFunctions].lookUp {
                        PrioritizedLookupElement.withPriority(
                            bold(),
                            90.0
                        )
                    }
                )
                result.addAllElements(propName.parent<DTOPositiveProp>()[StructureType.PropProperties].lookUp())
            },
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(2, DTOPositiveProp::class.java)
                    .withSuperParent(3, DTODtoBody::class.java)
                    .andOr(
                        identifier.withSuperParent(4, psiElement(DTODto::class.java)),
                        identifier.withSuperParent(
                            4,
                            psiElement(DTOPropBody::class.java)
                                    .andNot(
                                        psiElement(DTOPropBody::class.java)
                                                .afterSiblingSkipping(
                                                    or(
                                                        psiElement(DTOAnnotation::class.java),
                                                        psiElement(DTOPropArgs::class.java)
                                                    ),
                                                    psiElement(DTOPropName::class.java).withText("flat")
                                                )
                                    )
                        )
                    )
        )
    }

    /**
     * 负属性名提示
     */
    private fun completeNegativeProp() {
        complete(
            { parameters, result ->
                val propName = parameters.parent<DTOPropName>()
                result.addAllElements(propName.parent<DTONegativeProp>()[StructureType.PropNegativeProperties].lookUp())
            },
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(2, DTONegativeProp::class.java)
                    .afterLeaf(psiElement(DTOTypes.MINUS))
        )
    }

    /**
     * 宏提示
     */
    private fun completeMacro() {
        complete(
            { _, result ->
                result.addAllElements(listOf("allScalars").lookUp())
            },
            or(
                identifier.withParent(
                    psiElement(DTOMacroName::class.java)
                            .afterLeaf(psiElement(DTOTypes.HASH))
                ),
                identifier.withParent(DTOFile::class.java)
                        .afterLeaf(psiElement(DTOTypes.HASH))
            )
        )
        complete(
            { parameters, result ->
                val macroArgs = parameters.position.parent.parent.parent<DTOMacro>()
                result.addAllElements(macroArgs[StructureType.MacroTypes].lookUp())
            },
            identifier.withParent(psiElement(DTOMacroArg::class.java)),
        )
    }

    /**
     * 枚举提示
     */
    private fun completeEnum() {
        complete(
            { parameters, result ->
                result.addAllElements(parameters.parent<DTOEnumInstance>()[StructureType.EnumValues].lookUp())
            },
            identifier.withParent(DTOEnumInstance::class.java)
                    .withSuperParent(3, psiElement(DTOEnumBody::class.java))
        )
    }

    /**
     * Dto修饰符提示
     */
    private fun completeDtoModifier() {
        complete(
            { parameters, result ->
                result.addAllElements(
                    parameters.parent<DTODtoName>()[StructureType.DtoModifiers].lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            identifier.withParent(DTODtoName::class.java)
                    .withSuperParent(2, DTODto::class.java)
                    .withSuperParent(3, DTOFile::class.java)
        )
    }

    /**
     * 正属性修饰符提示
     */
    private fun completePositivePropModifier() {
        complete(
            { _, result ->
                result.addAllElements(
                    Modifier.values()
                            .filter { it.level == Modifier.Level.Both }
                            .map { it.name.lowercase() }
                            .lookUp {
                                PrioritizedLookupElement.withPriority(bold(), 100.0)
                            }
                )
            },
            and(
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(
                            2,
                            psiElement(DTOPositiveProp::class.java)
                                    .beforeLeaf(
                                        identifier.withSuperParent(
                                            2,
                                            psiElement(DTOPositiveProp::class.java),
                                        )
                                    )
                        ),
                identifier.withParent(
                    not(
                        psiElement(DTOPropName::class.java).afterSibling(psiElement(DTOModifier::class.java))
                    )
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
                val propArgs = parameters.parent<DTOValue>().parent as DTOPropArgs
                result.addAllElements(propArgs[StructureType.FunctionArgs].lookUp())
            },
            identifier.withParent(DTOValue::class.java)
                    .withSuperParent(
                        2,
                        psiElement(DTOPropArgs::class.java)
                                .afterSiblingSkipping(
                                    psiElement(DTOPropFlags::class.java),
                                    psiElement(DTOPropName::class.java)
                                )
                    )
        )
    }

    /**
     * Flat方法体提示
     */
    private fun completeFlatBody() {
        complete(
            { parameters, result ->
                val flatArgs = (parameters.parent<DTOPropName>().parent.parent.parent.parent as DTOPositiveProp).propArgs
                result.addAllElements(flatArgs?.valueList?.get(0)?.get(StructureType.FlatProperties)?.lookUp() ?: emptyList())
                result.addAllElements(
                    parameters.parent<DTOPropName>()[StructureType.PropFunctions].lookUp {
                        PrioritizedLookupElement.withPriority(
                            bold(),
                            90.0
                        )
                    }
                )
                result.addAllElements(bodyLookups())
            },
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(
                        4,
                        psiElement(DTOPropBody::class.java)
                                .afterSiblingSkipping(
                                    or(
                                        psiElement(DTOAnnotation::class.java),
                                        psiElement(DTOPropArgs::class.java),
                                    ),
                                    psiElement(DTOPropName::class.java).withText("flat"),
                                )
                    )
        )
    }

    /**
     * AliasGroup方法体提示
     */
    private fun completeAsBody() {
        complete(
            { parameters, result ->
                val macros = listOf(
                    LookupInfo(
                        "#allScalars",
                        "(Type+)",
                        "macro",
                        "#allScalars"
                    )
                ).lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
                result.addAllElements(macros)

                val aliasGroup = parameters.parent<DTOPropName>().parent.parent.parent as DTOAliasGroup
                result.addAllElements(aliasGroup[StructureType.AsProperties].lookUp())
            },
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(4, psiElement(DTOAliasGroup::class.java))
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
            identifier.withSuperParent(2, DTODto::class.java)
                    .inFile(psiFile(DTOFile::class.java).withFirstNonWhitespaceChild(psiElement(DTODto::class.java)))
        )
    }

    /**
     * Import关键字提示
     */
    private fun completeImportKeyword() {
        val dto = identifier.withSuperParent(2, DTODto::class.java)
        complete(
            { _, result ->
                result.addAllElements(
                    listOf("import").lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            dto.inFile(
                psiFile(DTOFile::class.java).withFirstChildSkipping(
                    or(
                        psiElement(DTOExportStatement::class.java),
                        whitespace,
                        psiElement(DTOImportStatement::class.java),
                    ),
                    or(
                        psiElement(DTODto::class.java)
                                .withChild(psiElement(DTODtoName::class.java))
                                .withText(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED),
                        psiElement(DTODto::class.java)
                                .withChild(psiElement(DTODtoName::class.java))
                                .withText(string().atStart("import")),
                    )
                )
            )
        )
    }

    /**
     * Export包提示
     */
    private fun completeExportPackage() {
        completePackage(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(4, DTOExportStatement::class.java),
            DTOTypes.EXPORT,
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
                    .withSuperParent(4, DTOImportStatement::class.java),
            DTOTypes.IMPORT,
            Project::allClasses
        )
    }

    /**
     * 注解提示
     */
    private fun completeAnnotation() {
        completePackage(
            or(
                identifier.withParent(DTOAnnotationName::class.java)
                        .withSuperParent(3, DTOAnnotation::class.java),
                identifier.withParent(DTOFile::class.java)
                        .afterLeafSkipping(
                            or(
                                psiElement(DTOTypes.DOT),
                                identifier,
                            ),
                            psiElement(DTOTypes.AT),
                        ),
            ),
            DTOTypes.AT,
            Project::allAnnotations,
            true,
        )
    }

    /**
     * 嵌套注解提示
     */
    private fun completeNestAnnotation() {
        complete(
            { parameters, result ->
                val nestAnnotation = parameters.position.parent.parent<DTONestAnnotation>()
                val nestAnnotationParam = nestAnnotation.parent.parent.parent.parent<DTOAnnotationParameter>()
                val annotation = nestAnnotationParam.parent<DTOAnnotation>()
                val annotationClass = annotation.annotationConstructor.annotationName!!.psiClass()

                result.addAllElements(
                    annotationClass.methods
                            .filter { it.name == nestAnnotationParam.identifier.text }
                            .mapNotNull { it.returnType }
                            .mapNotNull { it.clazz() }
                            .filter(PsiClass::isAnnotationType)
                            .lookUp()
                )
            },
            or(
                psiElement().withParent(DTOAnnotationArrayValue::class.java),
                identifier.withParent(psiElement(DTOAnnotationName::class.java).withParent(DTONestAnnotation::class.java)),
            ),
        )
    }

    /**
     * 包和类提示
     *
     * @param classAvailableBeforeFirstDot 类提示是否在全限定类名中的第一段可用
     */
    private inline fun completePackage(
        place: ElementPattern<PsiElement>,
        statementKeyword: IElementType,
        crossinline classes: Project.(String?) -> List<PsiClass>,
        classAvailableBeforeFirstDot: Boolean = false,
        needImport: Boolean = true,
    ) {
        complete(
            { parameters, result ->
                val project = parameters.position.project
                val typedPackage = if (parameters.position.parent is DTOAnnotationName) {
                    parameters.position.prevLeafs
                            .takeWhile { it.elementType != statementKeyword }
                            .filter { it.elementType != TokenType.WHITE_SPACE }
                            .map(PsiElement::getText)
                            .filter { it != "." }
                            .toList()
                            .asReversed()
                } else {
                    parameters.position.parent.prevLeafs
                            .takeWhile { it.elementType != statementKeyword }
                            .filter { it.elementType != TokenType.WHITE_SPACE }
                            .filter { it.parent.elementType == DTOTypes.QUALIFIED_NAME_PART }
                            .map(PsiElement::getText)
                            .toList()
                            .asReversed()
                }
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
            place
        )
    }

    /**
     * 注解参数提示
     */
    private fun completeAnnotationParam() {
        complete(
            { parameters, result ->
                val annotation = parameters.position.parent.parent<DTOAnnotation>()
                result.addAllElements(annotation[StructureType.AnnotationParameters].lookUp())
            },
            identifier.withParent(
                psiElement(DTOAnnotationParameter::class.java)
                        .afterSiblingSkipping(
                            or(
                                psiElement(DTOTypes.PAREN_L),
                                psiElement(DTOAnnotationValue::class.java),
                                psiElement(DTOAnnotationParameter::class.java),
                            ),
                            psiElement(DTOAnnotationConstructor::class.java),
                        ),
            ),
        )
    }

    /**
     * 嵌套注解参数提示
     */
    private fun completeNestAnnotationParam() {
        complete(
            { parameters, result ->
                val nestAnnotation = parameters.position.parent.parent<DTONestAnnotation>()
                result.addAllElements(nestAnnotation[StructureType.NestAnnotationParameters].lookUp())
            },
            identifier.withParent(
                psiElement(DTOAnnotationParameter::class.java)
                        .withParent(
                            psiElement(DTONestAnnotation::class.java)
                                    .withParent(
                                        psiElement(DTOAnnotationValue::class.java)
                                                .withParent(DTOAnnotationArrayValue::class.java)
                                    ),
                        ),
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
                identifier.withParent(DTOEnumValue::class.java),
                identifier.withParent(error.afterSibling(psiElement(DTOTypes.CLASS_REFERENCE))),
                identifier.withParent(DTOFile::class.java)
                        .afterSibling(
                            or(
                                psiElement(DTOTypes.CLASS_REFERENCE),
                                psiElement(DTOTypes.DOT)
                            )
                        )
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
                identifier.withParent(DTODtoName::class.java)
                        .withSuperParent(
                            2,
                            psiElement(DTODto::class.java).afterSiblingSkipping(
                                whitespace,
                                psiElement(DTODto::class.java),
                            )
                        ),
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(
                            2,
                            psiElement(DTOPositiveProp::class.java)
                                    .afterSibling(
                                        psiElement(DTOPositiveProp::class.java)
                                                .andNot(
                                                    psiElement(DTOPositiveProp::class.java)
                                                            .withChild(psiElement(DTOPropArgs::class.java))
                                                )
                                    )
                        )
            )
        )
    }

    /**
     * 属性配置提示
     */
    private fun completePropConfig() {
        complete(
            { parameters, result ->
                val propConfig = parameters.position.parentUnSure<DTOPropConfig>() ?: return@complete
                val prop = propConfig.parent<DTOPositiveProp>()
                val dtoFile = prop.containingFile as DTOFile
                val propPath = prop.propPath()
                val clazz = dtoFile.findClass(propPath)
                val property = dtoFile.findProperty(propPath)

                val haveFilter = prop.hasConfig(PropConfigName.Filter)
                val haveWhere = prop.hasConfig(PropConfigName.Where)
                val haveOrderBy = prop.hasConfig(PropConfigName.OrderBy)
                val haveDepth = prop.hasConfig(PropConfigName.Depth)
                val haveRecursion = prop.hasConfig(PropConfigName.Recursion)

                val isEntityAssociation = property.doesTypeHaveAnnotation(Entity::class)
                val isAssociation = property.doesTypeHaveAnnotation(Immutable::class) ||
                        property.doesTypeHaveExactlyOneAnnotation(
                            Entity::class,
                            MappedSuperclass::class,
                            Embeddable::class,
                        )
                val propertyIsList = property.type is LType.CollectionType
                val propertyIsReference = !propertyIsList && isAssociation

                val availableProps = PropConfigName.values().toMutableList()

                if (haveFilter || !isEntityAssociation || propertyIsReference && !property.nullable) {
                    availableProps -= PropConfigName.Where
                }

                if (haveFilter || !isEntityAssociation || !propertyIsList) {
                    availableProps -= PropConfigName.OrderBy
                }

                if (haveWhere || haveOrderBy || !isEntityAssociation || !propertyIsList) {
                    availableProps -= PropConfigName.Filter
                }

                if (haveDepth || property.actualType != clazz) {
                    availableProps -= PropConfigName.Recursion
                }

                if (!isEntityAssociation || propertyIsList) {
                    availableProps -= PropConfigName.FetchType
                }

                if (!isEntityAssociation || !propertyIsList) {
                    availableProps -= PropConfigName.Limit
                }

                if (!propertyIsList) {
                    availableProps -= PropConfigName.Batch
                }

                if (haveRecursion || property.actualType != clazz) {
                    availableProps -= PropConfigName.Depth
                }

                result.addAllElements(availableProps.map(PropConfigName::text).lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) })
            },
            psiElement(DTOTypes.PROP_CONFIG_NAME),
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

    private fun findUserPropType(file: DTOFile, isGeneric: Boolean = false): List<LookupElement> {
        val basicTypes = BasicType.types().lookUp()
        val genericTypes = GenericType.types().lookUp()
        val classes = PsiShortNamesCache.getInstance(file.project).allClassNames.toList().lookUp()

        val genericModifiers = if (isGeneric) {
            listOf("out", "in").lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        } else {
            emptyList()
        }
        val imports = file[StructureType.DTOFileImports].lookUp()
        return basicTypes +
                genericTypes +
                genericModifiers +
                imports +
                classes
    }

    private fun bodyLookups(): List<LookupElement> {
        val macros = listOf(
            LookupInfo(
                "#allScalars",
                "(Type+)",
                "macro",
                "#allScalars"
            )
        ).lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        val aliasGroup = listOf(
            LookupInfo(
                "as",
                "(<original> -> <replacement>) { ... }",
                "alias-group",
                "as() {}",
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
    private fun List<Property>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            val nullable = it.nullable
            LookupElementBuilder.create(it.name)
                    .withTypeText(it.type + if (nullable) "?" else "", true)
                    .customizer()
        }
    }

    @JvmName("lookupPsiClass")
    private fun List<PsiClass>.lookUp(needImport: Boolean = true, customizer: LookupElementBuilder.() -> LookupElement = { this }) = map {
        val qualifiedName = it.qualifiedName!!
        val name = it.name!!
        LookupElementBuilder.create(qualifiedName, name)
                .withIcon(it.icon)
                .withTypeText("(${qualifiedName.substringBeforeLast('.')})", true)
                .withInsertHandler { context, _ ->
                    if (needImport) {
                        WriteCommandAction.runWriteCommandAction(context.project) {
                            val file = context.file as DTOFile
                            val export = file.findChildByClass(DTOExportStatement::class.java)
                            val imports = file.findChildrenByClass(DTOImportStatement::class.java)
                            val importedSameName = imports.any { i -> i.qualified.substringAfterLast('.') == name }
                            val import = imports.find { i -> i.qualified == qualifiedName }

                            if (importedSameName) {
                                if (import == null) {
                                    val annotationName = file.findElementAt(context.startOffset)?.parent ?: return@runWriteCommandAction
                                    val newAnnotationName = context.project.createAnnotation(qualifiedName).annotationConstructor.annotationName
                                    annotationName.parent.node.replaceChild(annotationName.node, newAnnotationName!!.node)
                                }
                            } else {
                                if (import == null) {
                                    if (imports.isEmpty()) {
                                        if (export == null) {
                                            file.node.addLeaf(TokenType.WHITE_SPACE, "import $qualifiedName\n\n", file.node.firstChildNode)
                                        } else {
                                            file.node.addLeaf(DTOTypes.IMPORT, "\n\nimport $qualifiedName", export.node.treeNext)
                                        }
                                    } else {
                                        file.node.addLeaf(DTOTypes.IMPORT, "\nimport $qualifiedName", imports.last().node.treeNext)
                                    }
                                }
                            }
                        }
                    }
                }
                .customizer()
    }

    @JvmName("lookupPsiMethod")
    private fun List<PsiMethod>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }) = map {
        LookupElementBuilder.create(it.name)
                .withIcon(AllIcons.Nodes.Property)
                .withTypeText(it.returnType?.canonicalText, true)
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

    private inline fun <reified T> CompletionParameters.parent(): T {
        return position.parent as T
    }
}
