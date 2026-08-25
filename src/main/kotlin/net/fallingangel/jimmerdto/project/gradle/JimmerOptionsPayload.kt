package net.fallingangel.jimmerdto.project.gradle

import com.intellij.serialization.PropertyMapping

class JimmerOptionsPayload @PropertyMapping("raw") constructor(val raw: Map<String, String>)
