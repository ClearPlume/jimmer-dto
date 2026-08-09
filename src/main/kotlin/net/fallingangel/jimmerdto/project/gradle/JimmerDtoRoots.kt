package net.fallingangel.jimmerdto.project.gradle

import com.intellij.serialization.PropertyMapping
import net.fallingangel.jimmerdto.project.sourceroot.DtoSourceRootType
import java.io.Serializable

class JimmerDtoRoots @PropertyMapping("dirs", "typeId") constructor(val dirs: List<String>, val typeId: String) : Serializable {
    operator fun component1() = dirs
    operator fun component2() = DtoSourceRootType.fromTypeId(typeId)
}