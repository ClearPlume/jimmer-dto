package net.fallingangel.jimmerdto.project.maven

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import net.fallingangel.jimmerdto.project.ProjectSyncTracker
import org.jetbrains.idea.maven.project.MavenImportListener
import org.jetbrains.idea.maven.project.MavenProject

class MavenSyncListener(private val project: Project) : MavenImportListener {
    override fun importFinished(importedProjects: Collection<MavenProject>, newModules: List<Module>) {
        ProjectSyncTracker.getInstance(project).incModificationCount()
    }
}