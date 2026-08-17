package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.enums.AUTO_IMPORTED_TYPES
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.missing
import net.fallingangel.jimmerdto.psi.resolve.Resolution

fun List<Resolution.Candidate>.lookUp(insideImportMechanism: Boolean): List<LookupElement> {
    return map { it.lookUp(insideImportMechanism) }
}

fun Resolution.Candidate.lookUp(insideImportMechanism: Boolean): LookupElement {
    return when (target) {
        is Resolution.Target.Pkg -> LookupElementBuilder.createWithIcon(target.`package`)
        is Resolution.Target.Type -> target.type.lookUp(name, insideImportMechanism)

        is Resolution.Target.Property -> {
            val property = target.property
            LookupElementBuilder.create(target.property.dependencyItem, property.name)
                .withIcon(property.dependencyItem.getIcon(0))
        }

        is Resolution.Target.EnumConst -> LookupElementBuilder.createWithIcon(target.enum)

        is Resolution.Target.Alias -> {
            target.target
                ?.let {
                    val element = it.type
                    val packageName = process(element) { className().pkg } ?: element.missing("className")
                    LookupElementBuilder.create(element, name)
                        .withIcon(element.getIcon(0))
                        .withTailText(" -> ${element.name}")
                        .withTypeText("($packageName)", true)
                }
                ?: LookupElementBuilder.create(name)
        }

        is Resolution.Target.Subtype -> LookupElementBuilder.createWithIcon(target.lClass.dependencyItem)
    }
}

fun PsiNamedElement.lookUp(name: String, insideImportMechanism: Boolean): LookupElement {
    val className = process(this) { className() } ?: missing("className")
    val builtin = className.fqName in AUTO_IMPORTED_TYPES

    return LookupElementBuilder.create(this, name)
        .withIcon(getIcon(0))
        .withTypeText(if (builtin) "(built-in)" else "(${className.pkg})", true)
        .withInsertHandler { context, _ ->
            if (insideImportMechanism && !builtin) {
                (context.file as DTOFile).addImport(className.fqName)
            }
        }
}
