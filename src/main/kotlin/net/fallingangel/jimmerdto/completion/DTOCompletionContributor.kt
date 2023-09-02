package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.psi.DTOImportStatement
import net.fallingangel.jimmerdto.psi.DTOTypes

class DTOCompletionContributor : CompletionContributor() {
    init {
        completeUserPropType()
        completeUserPropGenericType()
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
        val identifier = psiElement(DTOTypes.IDENTIFIER)
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

    private fun List<String>.lookUp(customizer: LookupElementBuilder.() -> LookupElement = { this }): List<LookupElement> {
        return map { LookupElementBuilder.create(it).customizer() }
    }
}
