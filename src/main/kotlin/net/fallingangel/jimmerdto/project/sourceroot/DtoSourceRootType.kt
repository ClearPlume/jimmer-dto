package net.fallingangel.jimmerdto.project.sourceroot

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.ex.JpsElementTypeWithDummyProperties
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

class DtoSourceRootType private constructor(
    val forTests: Boolean,
    val typeId: String,
) : JpsModuleSourceRootType<JpsDummyElement>, JpsElementTypeWithDummyProperties() {
    override fun isForTests() = forTests

    companion object {
        val SOURCE = DtoSourceRootType(false, "jimmer-dto-source")
        val TEST_SOURCE = DtoSourceRootType(true, "jimmer-dto-test-source")
    }
}

fun Module.dtoSourceRoots(includeTests: Boolean = false): List<VirtualFile> {
    val manager = ModuleRootManager.getInstance(this)
    return if (includeTests) {
        manager.getSourceRoots(setOf(DtoSourceRootType.SOURCE, DtoSourceRootType.TEST_SOURCE))
    } else {
        manager.getSourceRoots(DtoSourceRootType.SOURCE)
    }
}

fun VirtualFile.inDtoSourceRoot(project: Project): Boolean {
    return ProjectFileIndex.getInstance(project).isUnderSourceRootOfType(this, setOf(DtoSourceRootType.SOURCE, DtoSourceRootType.TEST_SOURCE))
}