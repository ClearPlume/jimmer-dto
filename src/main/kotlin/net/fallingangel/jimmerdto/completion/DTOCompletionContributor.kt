package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.properties

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
    }

    /**
     * 用户属性类型提示
     */
    private fun completeUserPropType() {
        extend(
            CompletionType.BASIC,
            psiElement(DTOTypes.IDENTIFIER)
                    .afterLeaf(psiElement(DTOTypes.COLON)),
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
            identifier
                    .andOr(
                        identifier.afterLeaf(psiElement(DTOTypes.ANGLE_BRACKET_L)),
                        identifier.afterLeaf(psiElement(DTOTypes.MODIFIER))
                    ),
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
        extend(
            CompletionType.BASIC,
            identifier.withParent(DTOPropName::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val dtoFile = parameters.originalFile
                    result.addAllElements(
                        listOf("id", "flat")
                                .map { Property(it, "function") }
                                .lookUp {
                                    PrioritizedLookupElement.withPriority(bold(), 100.0)
                                }
                    )
                    result.addAllElements(
                        listOf("as")
                                .map { Property(it, "alias-group") }
                                .lookUp {
                                    PrioritizedLookupElement.withPriority(bold(), 90.0)
                                }
                    )
                    result.addAllElements(dtoFile.virtualFile.properties(dtoFile.project).lookUp())
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
                    .afterLeafSkipping(psiElement(TokenType.WHITE_SPACE), psiElement(DTOTypes.MINUS)),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val dtoFile = parameters.originalFile
                    result.addAllElements(dtoFile.virtualFile.properties(dtoFile.project).lookUp())
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
    }

    @JvmName("lookUpString")
    private fun List<String>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map { LookupElementBuilder.create(it).customizer() }
    }

    private fun List<Property>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map {
            LookupElementBuilder.create(it.name)
                    .withTypeText(it.type, true)
                    .customizer()
        }
    }
}
