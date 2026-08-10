package net.fallingangel.jimmerdto.project.sourceroot

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
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

        fun fromTypeId(typeId: String): DtoSourceRootType {
            return when (typeId) {
                "jimmer-dto-source" -> SOURCE
                "jimmer-dto-test-source" -> TEST_SOURCE
                else -> throw IllegalArgumentException("unknown typeId: $typeId")
            }
        }
    }
}

fun Module.dtoSourceRoots(vararg types: DtoSourceRootType): List<VirtualFile> {
    val manager = ModuleRootManager.getInstance(this)
    return manager.getSourceRoots(types.toSet())
}