package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.completion.resolve.StructureType
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.get

class DTOCompletionContributor : CompletionContributor() {
    private val identifier = psiElement(DTOTypes.IDENTIFIER)

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

        // 继承提示
        completeExtend()

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
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val parent = element.parent
        when {
            parent is DTOEnumInstanceValue -> context.dummyIdentifier = ""
            parent is DTOEnumInstanceMapping && element.prevSibling.elementType == DTOTypes.COLON -> context.dummyIdentifier = ""
            parent.parent is DTOMacroArgs -> context.dummyIdentifier = ""
            parent.parent is DTOPropArgs -> context.dummyIdentifier = ""
            parent.parent is DTODtoSupers -> context.dummyIdentifier = ""
            else -> return
        }
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        complete(
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, DTOTypeDef::class.java)
                    .withSuperParent(3, DTOUserProp::class.java)
        ) { parameters, result ->
            result.addAllElements(findUserPropType(parameters.originalFile))
        }
    }

    /**
     * 用户属性类型中的泛型提示
     */
    private fun completeUserPropGenericType() {
        complete(
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, DTOTypeDef::class.java)
                    .withSuperParent(3, DTOGenericArg::class.java)
        ) { parameters, result ->
            result.addAllElements(findUserPropType(parameters.originalFile, true))
        }
    }

    /**
     * 正属性提示
     */
    private fun completeProp() {
        complete(
            identifier.withParent(DTOPropName::class.java)
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
                                                        psiElement(TokenType.WHITE_SPACE),
                                                        psiElement(DTOAnnotation::class.java),
                                                        psiElement(DTOPropArgs::class.java)
                                                    ),
                                                    psiElement(DTOPropName::class.java).withText("flat")
                                                )
                                    )
                        )
                    )
        ) { parameters, result ->
            result.addAllElements(bodyLookups())
            result.addAllElements(
                parameters.parent<DTOPropName>()[StructureType.PropFunctions].lookUp {
                    PrioritizedLookupElement.withPriority(
                        bold(),
                        90.0
                    )
                }
            )
            result.addAllElements(parameters.parent<DTOPropName>()[StructureType.PropProperties].lookUp())
        }
    }

    /**
     * 负属性名提示
     */
    private fun completeNegativeProp() {
        complete(
            identifier.withParent(DTONegativeProp::class.java)
                    .afterLeafSkipping(psiElement(TokenType.WHITE_SPACE), psiElement(DTOTypes.MINUS))
        ) { parameters, result ->
            result.addAllElements(parameters.parent<DTONegativeProp>()[StructureType.PropNegativeProperties].lookUp())
        }
    }

    /**
     * 宏提示
     */
    private fun completeMacro() {
        complete(identifier.withParent(DTOMacroName::class.java)) { _, result ->
            result.addAllElements(listOf("allScalars").lookUp())
        }
        complete(
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, psiElement(DTOMacroArgs::class.java))
        ) { parameters, result ->
            val macroArgs = parameters.parent<DTOQualifiedName>().parent as DTOMacroArgs
            result.addAllElements(macroArgs[StructureType.MacroTypes].lookUp())
        }
    }

    /**
     * Dto继承提示
     */
    private fun completeExtend() {
        complete(
            identifier.withParent(DTODtoName::class.java)
                    .withSuperParent(2, psiElement(DTODtoSupers::class.java))
        ) { parameters, result ->
            val supers = parameters.parent<DTODtoName>().parent as DTODtoSupers
            result.addAllElements(supers[StructureType.DtoSupers].lookUp())
        }
    }

    /**
     * 枚举提示
     */
    private fun completeEnum() {
        complete(
            identifier.withParent(DTOEnumInstance::class.java)
                    .withSuperParent(3, psiElement(DTOEnumBody::class.java))
        ) { parameters, result ->
            result.addAllElements(parameters.parent<DTOEnumInstance>()[StructureType.EnumValues].lookUp())
        }
    }

    /**
     * Dto修饰符提示
     */
    private fun completeDtoModifier() {
        complete(
            identifier.withParent(DTODtoName::class.java)
                    .withSuperParent(2, DTODto::class.java)
                    .withSuperParent(3, DTOFile::class.java)
        ) { parameters, result ->
            result.addAllElements(
                parameters.parent<DTODtoName>()[StructureType.DtoModifiers].lookUp {
                    PrioritizedLookupElement.withPriority(bold(), 100.0)
                }
            )
        }
    }

    /**
     * 提示方法参数
     */
    private fun completeFunctionParameter() {
        complete(
            identifier.withParent(DTOValue::class.java)
                    .withSuperParent(
                        2,
                        psiElement(DTOPropArgs::class.java)
                                .afterSiblingSkipping(
                                    or(
                                        psiElement(TokenType.WHITE_SPACE),
                                        psiElement(DTOPropFlags::class.java)
                                    ),
                                    psiElement(DTOPropName::class.java)
                                )
                    )
        ) { parameters, result ->
            val propArgs = parameters.parent<DTOValue>().parent as DTOPropArgs
            result.addAllElements(propArgs[StructureType.FunctionArgs].lookUp())
        }
    }

    /**
     * Flat方法体提示
     */
    private fun completeFlatBody() {
        complete(
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(
                        5,
                        psiElement(DTOPropBody::class.java)
                                .afterSiblingSkipping(
                                    or(
                                        psiElement(TokenType.WHITE_SPACE),
                                        psiElement(DTOAnnotation::class.java),
                                        psiElement(DTOPropArgs::class.java)
                                    ),
                                    psiElement(DTOPropName::class.java).withText("flat")
                                )
                    )
        ) { parameters, result ->
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
        }
    }

    /**
     * AliasGroup方法体提示
     */
    private fun completeAsBody() {
        complete(
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(4, psiElement(DTOAliasGroup::class.java))
        ) { parameters, result ->
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
        }
    }

    /**
     * 提示指定位置的内容
     *
     * @param place 元素位置表达式
     * @param provider 内容提示
     */
    private fun complete(place: ElementPattern<PsiElement>, provider: (CompletionParameters, CompletionResultSet) -> Unit) {
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
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(file, DTOImportStatement::class.java)
        val importedTypes = imports.filter { it.alias == null && it.groupedTypes == null }.map { it.lastChild.text }.lookUp()
        val importedSingleAliasTypes = imports.filter { it.alias != null }.map { it.alias!!.identifier.text }.lookUp()
        val importedGroupedAliasTypes = imports
                .filter { it.groupedTypes != null }
                .map {
                    val groupedTypes = it.groupedTypes!!.groupedTypeList
                    val alias = groupedTypes.filter { type -> type.alias != null }.map { type -> type.alias!!.identifier.text }
                    val types = groupedTypes.filter { type -> type.alias == null }.map { type -> type.lastChild.text }
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
