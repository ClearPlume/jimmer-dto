package net.fallingangel.jimmerdto.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.codeStyle.CodeStyleManager
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createImport
import net.fallingangel.jimmerdto.psi.element.createQualifiedName
import net.fallingangel.jimmerdto.psi.missing
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.findChild
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
            LookupElementBuilder.create(target.property.dependencyItem, property.name)
                .withIcon(property.dependencyItem.getIcon(0))
        }

        is Resolution.Target.EnumConst -> LookupElementBuilder.createWithIcon(target.enum)

        is Resolution.Target.Alias -> {
            target.target
                ?.let {
                    val element = it.type
                    val packageName = process(element) { packageName() } ?: element.missing("packageName")
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
    val qualifiedName = process(this) { classQualifiedName() } ?: missing("qualifiedName")
    val packageName = process(this) { packageName() } ?: missing("packageName")

    return LookupElementBuilder.create(this, name)
        .withIcon(getIcon(0))
        .withTypeText("($packageName)", true)
        .withInsertHandler { context, _ ->
            if (insideImportMechanism) {
                val file = context.file as DTOFile
                val root = file.findChild<PsiElement>("/dtoFile")
                val project = file.project
                val export = file.export
                val imports = file.importStatements

                // 是否已经导入过相同简单名的类
                if (name in file.importIndex) {
                    // 已导入的类全限定名是否等于要导入的类
                    if (file.importIndex[name]?.singleOrNull()?.qualifiedName != qualifiedName) {
                        val annotationName = file.findElementAt(context.startOffset)?.parent<DTOQualifiedName>() ?: return@withInsertHandler
                        annotationName.replace(project.createQualifiedName(qualifiedName))
                    }
                } else {
                    val import = project.createImport(qualifiedName)

                    if (imports.isEmpty()) {
                        if (export == null) {
                            val inserted = root.addBefore(import, file.findChild("/dtoFile/dto"))
                            CodeStyleManager.getInstance(project).reformatRange(
                                root,
                                0,
                                inserted.textRange.endOffset,
                            )
                        } else {
                            val inserted = root.addAfter(import, export)
                            CodeStyleManager.getInstance(project).reformatRange(
                                root,
                                export.textRange.startOffset,
                                inserted.textRange.endOffset,
                            )
                        }
                    } else {
                        val inserted = root.addAfter(import, imports.last())
                        CodeStyleManager.getInstance(project).reformatRange(
                            root,
                            imports.last().textRange.startOffset,
                            inserted.textRange.endOffset,
                        )
                    }
                }
            }
        }
}
