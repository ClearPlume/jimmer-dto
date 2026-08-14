package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import net.fallingangel.jimmerdto.core.DTOFileType

val Project.dtoScope: GlobalSearchScope
    get() = GlobalSearchScope.getScopeRestrictedByFileTypes(
        GlobalSearchScope.allScope(this),
        DTOFileType.INSTANCE,
    )