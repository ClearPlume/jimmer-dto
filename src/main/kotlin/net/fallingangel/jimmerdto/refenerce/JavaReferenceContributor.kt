package net.fallingangel.jimmerdto.refenerce

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import net.fallingangel.jimmerdto.util.parentUnSure

class JavaReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(PsiLiteralExpression::class.java)
                    .withParent(PsiNameValuePair::class.java)
                    .withSuperParent(3, PsiAnnotation::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val prop = element.parent.parent.parent.parent.parentUnSure<PsiMethod>() ?: return PsiReference.EMPTY_ARRAY

                    val manyToMany = prop.getAnnotation("org.babyfish.jimmer.sql.ManyToMany") ?: return PsiReference.EMPTY_ARRAY
                    val mappedBy = manyToMany.parameterList.attributes.find { it.name == "mappedBy" } ?: return PsiReference.EMPTY_ARRAY

                    val targetType = prop.returnType as? PsiClassType ?: return PsiReference.EMPTY_ARRAY
                    val targetClass = if (targetType.hasParameters()) {
                        (targetType.parameters[0] as? PsiClassType)?.resolve()
                    } else {
                        targetType.resolve()
                    }
                    targetClass ?: return PsiReference.EMPTY_ARRAY

                    return arrayOf(
                        object : PsiReferenceBase<PsiElement>(element, TextRange(1, element.textLength - 1)) {
                            override fun resolve(): PsiElement? {
                                return targetClass.methods.find { it.name == mappedBy.literalValue }
                            }
                        }
                    )
                }
            },
        )
    }
}