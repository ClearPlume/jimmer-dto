package net.fallingangel.jimmerdto.enums

enum class RelationType(val whetherList: Boolean) {
    OneToOne(false),
    OneToMany(true),
    ManyToOne(false),
    ManyToMany(true),
    ManyToManyView(true);

    companion object {
        val all by lazy { values().map { it.name } }
        val singles by lazy { values().filterNot { it.whetherList }.map { it.name } }
        val lists by lazy { values().filter { it.whetherList }.map { it.name } }
    }
}