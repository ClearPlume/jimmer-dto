package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerTypes
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.haveParent
import net.fallingangel.jimmerdto.util.parent
import net.fallingangel.jimmerdto.util.psiClass

interface DTOQualifiedName : DTOElement {
    val parts: List<DTOQualifiedNamePart>

    val value: String
        get() = parts.joinToString(".", transform = DTOQualifiedNamePart::part)

    val `package`: String
        get() = parts.dropLast(1).joinToString(".", transform = DTOQualifiedNamePart::part)

    val simpleName: String
        get() = parts.last().part

    val initialSpace: Resolution.Space?
        get() {
            val parent = parent
            if (parent is DTOMorphism) {
                val lClass = parent.containingLClass ?: return null
                return Resolution.Space.Subtypes(file, lClass)
            }

            val config = parent<DTOPropConfig>()
            if (config != null) {
                return when (config.name.text) {
                    PropConfigName.Where.text, PropConfigName.OrderBy.text -> config.containingLClass?.let(Resolution.Space::Properties)

                    PropConfigName.FetchType.text -> JimmerTypes.ReferenceFetchType.psiClass()?.let(Resolution.Space::Type)

                    else -> Resolution.Space.GlobalWithImports(file, file.entityPackage)
                }
            }

            return if (haveParent<DTOImportStatement>() || haveParent<DTOExportStatement>()) {
                Resolution.Space.GlobalRaw(file)
            } else {
                Resolution.Space.GlobalWithImports(file, file.entityPackage)
            }
        }

    val target: Resolution.Target?
        get() = parts.lastOrNull()?.target
}
