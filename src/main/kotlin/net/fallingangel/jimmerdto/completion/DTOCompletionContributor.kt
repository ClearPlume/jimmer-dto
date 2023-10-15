package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
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

        // 方法提示
        completeFunction()

        // 继承提示
        completeExtend()
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, DTOTypeDef::class.java)
                    .withSuperParent(3, DTOUserProp::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(findUserPropType(parameters.originalFile))
                }
            }
        )
    }

    /**
     * 用户属性类型中的泛型提示
     */
    private fun completeUserPropGenericType() {
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, DTOTypeDef::class.java)
                    .withSuperParent(3, DTOGenericArg::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(findUserPropType(parameters.originalFile, true))
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

    /**
     * 正属性提示
     */
    private fun completeProp() {
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
        val functions = listOf(
            LookupInfo("id(<association>)", "id()", "function", -1),
            LookupInfo("flat(<association>) { ... }", "flat() {}", "function", -4)
        ).lookUp { PrioritizedLookupElement.withPriority(bold(), 80.0) }
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(5, DTOPropBody::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(functions + aliasGroup + macros)
                    result.addAllElements(parameters.parent<DTOPropName>()[StructureType.RelationProperties].lookUp())
                }
            }
        )

        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOPropName::class.java)
                    .withSuperParent(5, DTODto::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(functions + aliasGroup + macros)
                    result.addAllElements(parameters.parent<DTOPropName>()[StructureType.DtoProperties].lookUp())
                }
            }
        )
    }

    /**
     * 负属性名提示
     */
    private fun completeNegativeProp() {
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTONegativeProp::class.java)
                    .afterLeafSkipping(psiElement(TokenType.WHITE_SPACE), psiElement(DTOTypes.MINUS))
                    .withSuperParent(4, DTOPropBody::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(parameters.parent<DTONegativeProp>()[StructureType.NegativeRelationProperties].lookUp())
                }
            }
        )
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTONegativeProp::class.java)
                    .afterLeafSkipping(psiElement(TokenType.WHITE_SPACE), psiElement(DTOTypes.MINUS))
                    .withSuperParent(4, DTODto::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(parameters.parent<DTONegativeProp>()[StructureType.NegativeDtoProperties].lookUp())
                }
            }
        )
    }

    /**
     * 宏提示
     */
    private fun completeMacro() {
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOMacroName::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    result.addAllElements(listOf("allScalars").lookUp())
                }
            }
        )
        // DTO
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, psiElement(DTOMacroArgs::class.java))
                    .withSuperParent(6, psiElement(DTODto::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val macroArgs = parameters.parent<DTOQualifiedName>().parent as DTOMacroArgs
                    result.addAllElements(macroArgs[StructureType.MacroTypes].lookUp())
                }
            }
        )
        // 关联属性
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOQualifiedName::class.java)
                    .withSuperParent(2, psiElement(DTOMacroArgs::class.java))
                    .withSuperParent(6, psiElement(DTOPropBody::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val macroArgs = parameters.parent<DTOQualifiedName>().parent as DTOMacroArgs
                    result.addAllElements(macroArgs[StructureType.RelationMacroTypes].lookUp())
                }
            }
        )
    }

    /**
     * 方法参数提示
     */
    private fun completeFunction() {
        // DTO
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOValue::class.java)
                    .withSuperParent(2, psiElement(DTOPropArgs::class.java))
                    .withSuperParent(6, psiElement(DTODto::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val propArgs = parameters.parent<DTOValue>().parent as DTOPropArgs
                    result.addAllElements(propArgs[StructureType.PropArgs].lookUp())
                }
            }
        )
        // 关联属性
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOValue::class.java)
                    .withSuperParent(2, psiElement(DTOPropArgs::class.java))
                    .withSuperParent(6, psiElement(DTOPropBody::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val propArgs = parameters.parent<DTOValue>().parent as DTOPropArgs
                    result.addAllElements(propArgs[StructureType.RelationPropArgs].lookUp())
                }
            }
        )
    }

    /**
     * Dto继承提示
     */
    private fun completeExtend() {
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTODtoName::class.java)
                    .withSuperParent(2, psiElement(DTODtoSupers::class.java)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val supers = parameters.parent<DTODtoName>().parent as DTODtoSupers
                    result.addAllElements(supers[StructureType.DtoSupers].lookUp())
                }
            }
        )
    }

    @JvmName("lookupString")
    private fun List<String>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it)
                    .withCaseSensitivity(false)
                    .customizer()
        }
    }

    @JvmName("lookupProperty")
    private fun List<Property>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.name)
                    .withTypeText(it.type, true)
                    .withCaseSensitivity(false)
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
                    .withCaseSensitivity(false)
                    .customizer()
        }
    }

    private inline fun <reified T> CompletionParameters.parent(): T {
        return position.parent as T
    }
}
