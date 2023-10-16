package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.util.TextRange
import com.intellij.util.ProcessingContext

abstract class CompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val document = parameters.editor.document
        val offset = parameters.editor.caretModel.offset
        val matcherResult = result.withPrefixMatcher(document.getText(TextRange(offset - 1, offset)))
        completions(parameters, matcherResult)
    }

    abstract fun completions(parameters: CompletionParameters, result: CompletionResultSet)
}