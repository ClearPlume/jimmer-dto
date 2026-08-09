package net.fallingangel.jimmerdto.project

import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer

class DtoSourceRootSerializer : JpsModelSerializerExtension() {
    override fun getModuleSourceRootPropertiesSerializers(): List<JpsModuleSourceRootPropertiesSerializer<*>> {
        return listOf(
            DtoSourceRootPropertiesSerializer(DtoSourceRootType.SOURCE),
            DtoSourceRootPropertiesSerializer(DtoSourceRootType.TEST_SOURCE),
        )
    }
}