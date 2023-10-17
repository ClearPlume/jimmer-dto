package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext

abstract class CompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        completions(parameters, result.caseInsensitive())
    }

    abstract fun completions(parameters: CompletionParameters, result: CompletionResultSet)
}