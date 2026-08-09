package icons

import com.intellij.openapi.util.IconLoader

object Icons {
    @JvmField
    val PluginIcon = IconLoader.getIcon("/icons/pluginIcon.svg", javaClass)
    @JvmField
    val SourceRoot = IconLoader.getIcon("/icons/dtoSourceRoot.svg", javaClass)
    @JvmField
    val TestRoot = IconLoader.getIcon("/icons/dtoTestRoot.svg", javaClass)
}
