package net.fallingangel.jimmerdto.project

import org.jdom.Element
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer

class DtoSourceRootPropertiesSerializer(
    type: DtoSourceRootType,
) :
    JpsModuleSourceRootPropertiesSerializer<JpsDummyElement>(type, type.typeId) {
    override fun loadProperties(sourceRootTag: Element): JpsDummyElement {
        return JpsElementFactory.getInstance().createDummyElement()
    }

    override fun saveProperties(properties: JpsDummyElement, sourceRootTag: Element) {
    }
}