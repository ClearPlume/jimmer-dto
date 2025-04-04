package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.ElementType
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.parent
import org.babyfish.jimmer.sql.ManyToMany

class JavaCompletionContributor : CompletionContributor() {
    init {
        completeManyToMany()
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)
    }

    private fun completeManyToMany() {
        complete(
            { parameters, result ->
                val prop = parameters.position.parent.parent.parent.parent.parent.parent<PsiMethod>()

                val targetType = prop.returnType as? PsiClassType ?: return@complete
                val targetClass = if (targetType.hasParameters()) {
                    (targetType.parameters[0] as? PsiClassType)?.resolve()
                } else {
                    targetType.resolve()
                }
                targetClass ?: return@complete

                val targetProps = targetClass.methods
                        .filter { it.hasAnnotation(ManyToMany::class) }
                        .map {
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder.create(it)
                                        .withIcon(AllIcons.Nodes.Property)
                                        .withTypeText(it.returnType!!.presentableText),
                                100.0,
                            )
                        }
                result.addAllElements(targetProps)
            },
            psiElement(ElementType.STRING_LITERAL)
                    .withParent(PsiLiteralExpression::class.java)
                    .withSuperParent(2, PsiNameValuePair::class.java)
                    .withSuperParent(4, PsiAnnotation::class.java),
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
}