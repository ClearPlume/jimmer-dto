package net.fallingangel.jimmerdto.util

import java.util.*

val inspectionBundle: ResourceBundle = ResourceBundle.getBundle("inspection", Locale.PRC)

fun inspection(key: String): String = inspectionBundle.getString(key)
