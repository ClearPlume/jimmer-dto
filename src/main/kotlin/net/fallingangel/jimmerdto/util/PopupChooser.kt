package net.fallingangel.jimmerdto.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.components.JBList

fun Editor.popupChooser(title: String, values: List<String>, onOk: (String) -> Unit) {
    val valuesHolder = JBList(values)
    PopupChooserBuilder(valuesHolder)
            .setTitle(title)
            .addListener(object : JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    if (event.isOk) {
                        onOk(valuesHolder.selectedValue)
                    }
                }
            })
            .createPopup()
            .showInBestPositionFor(this)
}