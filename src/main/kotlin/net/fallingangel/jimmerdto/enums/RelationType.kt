package net.fallingangel.jimmerdto.enums

enum class RelationType(val whetherList: Boolean) {
    OneToOne(false),
    OneToMany(true),
    ManyToOne(false),
    ManyToMany(true),
    ManyToManyView(true);

    companion object {
        fun singles() = values().filterNot { it.whetherList }

        fun lists() = values().filterNot { it.whetherList }
    }
}