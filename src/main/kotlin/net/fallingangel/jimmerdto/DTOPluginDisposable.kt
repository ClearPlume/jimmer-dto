package net.fallingangel.jimmerdto

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DTOPluginDisposable : Disposable {
    override fun dispose() {
    }

    companion object {
        fun getInstance(project: Project) = project.service<DTOPluginDisposable>()
    }
}
