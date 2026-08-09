package icons

import com.intellij.openapi.util.IconLoader

object Icons {
    @JvmField
    val icon_16 = IconLoader.getIcon("/icon_16x16.svg", javaClass)
    @JvmField
    val SourceRoot = IconLoader.getIcon("/icons/dtoSourceRoot.svg", javaClass)
    @JvmField
    val TestRoot = IconLoader.getIcon("/icons/dtoTestRoot.svg", javaClass)
}
