package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class DTOCompletionContributor : CompletionContributor() {
    private val identifier = psiElement(DTOTypes.IDENTIFIER)
    private val whitespace = psiElement().whitespace()

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
                result.addAllElements(findUserPropType(parameters.originalFile))
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
                result.addAllElements(findUserPropType(parameters.originalFile, true))
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
        complete({ _, result ->
            result.addAllElements(listOf("allScalars").lookUp())
        }, identifier.withParent(DTOMacroName::class.java))
        complete(
            { parameters, result ->
                val macroArgs = if (parameters.position.elementType == DTOTypes.THIS_KEYWORD) {
                    parameters.parent<DTOMacroArgs>()
                } else {
                    parameters.parent<DTOQualifiedNamePart>().parent.parent as DTOMacroArgs
                }
                result.addAllElements(macroArgs[StructureType.MacroTypes].lookUp())
            },
            or(
                psiElement(DTOTypes.THIS_KEYWORD)
                        .withParent(DTOMacroArgs::class.java),
                identifier.withParent(DTOQualifiedNamePart::class.java)
                        .withSuperParent(2, psiElement(DTOQualifiedName::class.java))
                        .withSuperParent(3, psiElement(DTOMacroArgs::class.java))
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
                        "#allScalars[(Type+)]",
                        "#allScalars",
                        "macro"
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
                                .withText(string().with(object : PatternCondition<String>("atStart") {
                                    override fun accepts(str: String, context: ProcessingContext): Boolean {
                                        return "import".startsWith(str.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
                                    }
                                }))
                    )
                )
            )
        )
    }

    /**
     * Export包提示
     */
    private fun completeExportPackage() {
        completePackage<DTOExport>(
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
        completePackage<DTOImport>(
            identifier.withParent(DTOQualifiedNamePart::class.java)
                    .withSuperParent(5, DTOImport::class.java),
            DTOTypes.IMPORT_KEYWORD,
            Project::allClasses
        )
    }

    /**
     * 包提示
     */
    private inline fun <reified Statement : PsiElement> completePackage(
        place: ElementPattern<PsiElement>,
        statementKeyword: IElementType,
        crossinline classes: Project.(String) -> List<PsiClass>
    ) {
        complete(
            { parameters, result ->
                val parent = parameters.position.parent.parent.parent.parent.parent<Statement>()
                val project = parent.project

                val typedPackage = parameters.position.parent.prevLeafs
                        .takeWhile { it.elementType != statementKeyword }
                        .filter { it.parent.elementType == DTOTypes.QUALIFIED_NAME_PART }
                        .map { it.text }
                        .toList()
                        .reversed()
                val curPackage = typedPackage.joinToString(".")
                val curPackageClasses = project.classes(curPackage)
                        .map {
                            LookupElementBuilder.create(it.name!!)
                                    .withIcon(it.icon)
                                    .withTypeText("(${it.qualifiedName!!.substringBeforeLast('.')})", true)
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

    private fun findUserPropType(file: PsiFile, isGeneric: Boolean = false): List<LookupElement> {
        val embeddedTypes = listOf(
            "Boolean",
            "Boolean?",
            "Char",
            "Char?",
            "String",
            "String?",
            "Byte",
            "Byte?",
            "Short",
            "Short?",
            "Int",
            "Int?",
            "Float",
            "Float?",
            "Double",
            "Double?",
            "Long",
            "Long?",
            "Any",
            "Any?",
            "Array<>",
            "List<>",
            "MutableList<>",
            "Collection<>",
            "MutableCollection<>",
            "Iterable<>",
            "MutableIterable<>",
            "Set<>",
            "MutableSet<>",
            "Map<>",
            "MutableMap<>"
        ).lookUp()
        val genericModifiers = if (isGeneric) {
            listOf("out", "in").lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        } else {
            emptyList()
        }
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(file, DTOImport::class.java)
        val importedTypes = imports
                .filter { it.qualifiedType.qualifiedTypeAlias == null && it.groupedTypes == null }
                .map { it.lastChild.text }
                .lookUp()
        val importedSingleAliasTypes = imports
                .filter { it.qualifiedType.qualifiedTypeAlias != null }
                .map { it.qualifiedType.qualifiedTypeAlias!!.identifier.text }
                .lookUp()
        val importedGroupedAliasTypes = imports
                .filter { it.groupedTypes != null }
                .map {
                    val groupedTypes = it.groupedTypes!!.qualifiedTypeList
                    val alias = groupedTypes
                            .filter { type -> type.qualifiedTypeAlias != null }
                            .map { type -> type.qualifiedTypeAlias!!.identifier.text }
                    val types = groupedTypes
                            .filter { type -> type.qualifiedTypeAlias == null }
                            .map { type -> type.lastChild.text }
                    alias + types
                }
                .flatten()
                .lookUp()
        return embeddedTypes +
                genericModifiers +
                importedTypes +
                importedSingleAliasTypes +
                importedGroupedAliasTypes
    }

    private fun bodyLookups(): List<LookupElement> {
        val macros = listOf(
            LookupInfo(
                "#allScalars[(Type+)]",
                "#allScalars",
                "macro"
            )
        ).lookUp { PrioritizedLookupElement.withPriority(bold(), 100.0) }
        val aliasGroup = listOf(
            LookupInfo(
                "as(<original> -> <replacement>) { ... }",
                "as() {}",
                "alias-group",
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

    @JvmName("lookupInfo")
    private fun List<LookupInfo>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.insertion)
                    .withPresentableText(it.presentation)
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
