package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.structure.BasicType
import net.fallingangel.jimmerdto.structure.GenericType
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

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

        // Class关键字提示
        completeClassKeyword()

        // Implements关键字提示
        completeImplementsKeyword()

        // 注解提示
        completeAnnotation()
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val parent = element.parent
        when {
            parent is DTOEnumInstanceValue -> context.dummyIdentifier = ""
            parent is DTOEnumInstanceMapping
                    && element.prevSibling.elementType == DTOTypes.COLON -> context.dummyIdentifier = ""

            parent is DTOMacroArgs
                    || parent.parent is DTOMacroArgs -> context.dummyIdentifier = ""

            parent.parent is DTOPropArgs -> context.dummyIdentifier = ""
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
                    .withSuperParent(4, DTODtoBody::class.java)
                    .andOr(
                        identifier.withSuperParent(5, psiElement(DTODto::class.java)),
                        identifier.withSuperParent(
                            5,
                            psiElement(DTOPropBody::class.java)
                                    .andNot(
                                        psiElement(DTOPropBody::class.java)
                                                .afterSiblingSkipping(
                                                    or(
                                                        whitespace,
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
                    .afterLeafSkipping(whitespace, psiElement(DTOTypes.MINUS))
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
                            .afterLeafSkipping(whitespace, psiElement(DTOTypes.HASH))
                ),
                identifier.withParent(psiElement(DTOFile::class.java))
                        .afterLeafSkipping(whitespace, psiElement(DTOTypes.HASH))
            )
        )
        complete(
            { parameters, result ->
                val macroArgs = if (parameters.position.elementType == DTOTypes.THIS_KEYWORD) {
                    parameters.position.parent.parent<DTOMacroArgs>()
                } else {
                    parameters.parent<DTOQualifiedNamePart>().parent as DTOMacroArgs
                }
                result.addAllElements(macroArgs[StructureType.MacroTypes].lookUp())
            },
            or(
                psiElement(DTOTypes.THIS_KEYWORD)
                        .withParent(DTOMacroThis::class.java)
                        .withSuperParent(2, DTOMacroArgs::class.java),
                identifier.withParent(DTOQualifiedNamePart::class.java)
                        .withSuperParent(2, psiElement(DTOMacroArgs::class.java))
            )
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
                    Modifier.values().filter { it.level == Modifier.Level.Both }.map { it.name.lowercase() }.lookUp {
                        PrioritizedLookupElement.withPriority(bold(), 100.0)
                    }
                )
            },
            and(
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(
                            3,
                            psiElement(DTOExplicitProp::class.java)
                                    .afterSiblingSkipping(
                                        whitespace,
                                        psiElement(DTOExplicitProp::class.java)
                                                .withFirstNonWhitespaceChild(
                                                    psiElement(DTOPositiveProp::class.java)
                                                            .andNot(
                                                                psiElement(DTOPositiveProp::class.java)
                                                                        .withChild(psiElement(DTOPropArgs::class.java))
                                                            )
                                                )
                                    )
                        ),
                identifier.withParent(
                    not(
                        psiElement(DTOPropName::class.java)
                                .afterSiblingSkipping(
                                    whitespace,
                                    psiElement(DTOModifier::class.java)
                                )
                    )
                )
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
                                    or(
                                        whitespace,
                                        psiElement(DTOPropFlags::class.java)
                                    ),
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
                val flatArgs = (parameters.parent<DTOPropName>().parent.parent.parent.parent.parent as DTOPositiveProp).propArgs
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
                        5,
                        psiElement(DTOPropBody::class.java)
                                .afterSiblingSkipping(
                                    or(
                                        whitespace,
                                        psiElement(DTOAnnotation::class.java),
                                        psiElement(DTOPropArgs::class.java)
                                    ),
                                    psiElement(DTOPropName::class.java).withText("flat")
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
                        whitespace,
                        psiElement(DTOImport::class.java),
                        psiElement(DTOExport::class.java)
                    ),
                    or(
                        psiElement(DTODto::class.java)
                                .withChild(psiElement(DTODtoName::class.java))
                                .withText(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED),
                        psiElement(DTODto::class.java)
                                .withChild(psiElement(DTODtoName::class.java))
                                .withText(string().atStart("import"))
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
                    .withSuperParent(5, DTOExport::class.java),
            DTOTypes.EXPORT_KEYWORD,
            Project::allEntities
        )
    }

    /**
     * Import包提示
     */
    private fun completeImportPackage() {
        completePackage(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(5, DTOImport::class.java),
            DTOTypes.IMPORT_KEYWORD,
            Project::allClasses
        )
    }

    /**
     * 注解提示
     */
    private fun completeAnnotation() {
        completePackage(
            or(
                identifier.withParent(DTOQualifiedNamePart::class.java)
                        .withSuperParent(4, DTOAnnotation::class.java),
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
     * 包和类提示
     *
     * @param classAvailableBeforeFirstDot 类提示是否在全限定类名中的第一段可用
     */
    private inline fun completePackage(
        place: ElementPattern<PsiElement>,
        statementKeyword: IElementType,
        crossinline classes: Project.(String?) -> List<PsiClass>,
        classAvailableBeforeFirstDot: Boolean = false,
    ) {
        complete(
            { parameters, result ->
                val project = parameters.position.project
                val typedPackage = parameters.position.parent.prevLeafs
                        .takeWhile { it.elementType != statementKeyword }
                        .filter { it.parent.elementType == DTOTypes.QUALIFIED_NAME_PART }
                        .map { it.text }
                        .toList()
                        .asReversed()
                val curPackage = typedPackage.joinToString(".")
                val curPackageClasses = if (typedPackage.isEmpty() && classAvailableBeforeFirstDot) {
                    project.classes(null).lookUp()
                } else {
                    project.classes(curPackage).lookUp()
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
                identifier.withParent(error.afterSiblingSkipping(whitespace, psiElement(DTOTypes.CLASS_REFERENCE))),
                identifier.withParent(psiFile(DTOFile::class.java))
                        .afterSiblingSkipping(
                            whitespace,
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
                            psiElement(DTODto::class.java).afterSiblingSkipping(whitespace, psiElement(DTODto::class.java))
                        ),
                identifier.withParent(DTOPropName::class.java)
                        .withSuperParent(
                            3,
                            psiElement(DTOExplicitProp::class.java)
                                    .afterSiblingSkipping(
                                        whitespace,
                                        psiElement(DTOExplicitProp::class.java)
                                                .withFirstNonWhitespaceChild(
                                                    psiElement(DTOPositiveProp::class.java)
                                                            .andNot(
                                                                psiElement(DTOPositiveProp::class.java)
                                                                        .withChild(psiElement(DTOPropArgs::class.java))
                                                            )
                                                )
                                    )
                        )
            )
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
            LookupElementBuilder.create(it.name)
                    .withTypeText(it.type, true)
                    .customizer()
        }
    }

    @JvmName("lookupPsiClass")
    private fun List<PsiClass>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }) = map {
        val qualifiedName = it.qualifiedName!!
        val name = it.name!!
        LookupElementBuilder.create(qualifiedName, name)
                .withIcon(it.icon)
                .withTypeText("(${qualifiedName.substringBeforeLast('.')})", true)
                .withInsertHandler { context, _ ->
                    WriteCommandAction.runWriteCommandAction(context.project) {
                        val file = context.file as DTOFile
                        val export = file.findChildByClass(DTOExport::class.java)
                        val imports = file.findChildrenByClass(DTOImport::class.java)
                        val importedSameName = imports.any { i -> i.qualifiedType.text.substringAfterLast('.') == name }
                        val import = imports.find { i -> i.qualifiedType.text == qualifiedName }

                        if (importedSameName) {
                            if (import == null) {
                                val annotationName = file.findElementAt(context.startOffset)?.parent?.parent ?: return@runWriteCommandAction
                                val newAnnotationName = context.project.createAnnotation(qualifiedName).annotationConstructor.qualifiedName
                                annotationName.parent.node.replaceChild(annotationName.node, newAnnotationName.node)
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
