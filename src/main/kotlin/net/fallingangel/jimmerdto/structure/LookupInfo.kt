package net.fallingangel.jimmerdto.structure

/**
 * @param presentation 提示时看到的内容
 * @param insertion 确认提示后，实际插入的内容
 * @param type 提示时，条目的类型
 * @param tail 提示时，在条目名称之后的不重要信息，比如泛型
 * @param caretOffset 确认提示后，光标移动量
 */
data class LookupInfo(
    val presentation: String,
    val insertion: String = presentation,
    val type: String = "",
    val tail: String = "",
    val caretOffset: Int = 0
)
