package net.fallingangel.jimmerdto

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DTOPluginDisposable : Disposable {
    @Volatile
    var disposed: Boolean = false

    companion object {
        fun getInstance(project: Project): DTOPluginDisposable = project.service<DTOPluginDisposable>()
    }

    override fun dispose() {
        disposed = true
    }
}
