package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.enums.AUTO_IMPORTED_TYPES
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createQualifiedName
import net.fallingangel.jimmerdto.psi.missing
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.parent

fun List<Resolution.Candidate>.lookUp(insideImportMechanism: Boolean): List<LookupElement> {
    return map { it.lookUp(insideImportMechanism) }
}

fun Resolution.Candidate.lookUp(insideImportMechanism: Boolean): LookupElement {
    return when (target) {
        is Resolution.Target.Pkg -> LookupElementBuilder.createWithIcon(target.`package`)
        is Resolution.Target.Type -> target.type.lookUp(name, insideImportMechanism)

        is Resolution.Target.Property -> {
            val property = target.property
            LookupElementBuilder.create(target.property.dependencyItem, name)
                .withIcon(property.dependencyItem.getIcon(0))
                .withTypeText(property.presentableType, true)
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

        is Resolution.Target.Subtype -> target.lClass.dependencyItem.lookUp(name, insideImportMechanism)
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
                val file = context.file as DTOFile
                val qualifiedName = className.fqName

                // 是否已经导入过相同简单名的类
                if (name in file.importIndex) {
                    // 已导入的类全限定名是否等于要导入的类
                    if (file.importIndex[name]?.singleOrNull()?.qualifiedName != qualifiedName) {
                        val annotationName = file.findElementAt(context.startOffset)?.parent<DTOQualifiedName>() ?: return@withInsertHandler
                        annotationName.replace(project.createQualifiedName(qualifiedName))
                    }
                } else {
                    (context.file as DTOFile).addImport(className)
                }
            }
        }
}
