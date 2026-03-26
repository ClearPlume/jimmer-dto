package net.fallingangel.jimmerdto.util

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.open(project: Project, offset: Int = 0) {
    val openFileDescriptor = OpenFileDescriptor(project, this, offset)
    FileEditorManager.getInstance(project).openEditor(openFileDescriptor, true)
}