package net.fallingangel.jimmerdto.lsi.jimmer

import org.jetbrains.kotlin.name.ClassId

object JimmerAnnotations {
    val Entity = ClassId.fromString("org/babyfish/jimmer/sql/Entity")
    val MappedSuperclass = ClassId.fromString("org/babyfish/jimmer/sql/MappedSuperclass")
    val Embeddable = ClassId.fromString("org/babyfish/jimmer/sql/Embeddable")
    val Immutable = ClassId.fromString("org/babyfish/jimmer/Immutable")
    val TNullable = ClassId.fromString("org/babyfish/jimmer/client/TNullable")

    val Id = ClassId.fromString("org/babyfish/jimmer/sql/Id")
    val IdView = ClassId.fromString("org/babyfish/jimmer/sql/IdView")
    val Key = ClassId.fromString("org/babyfish/jimmer/sql/Key")
    val Formula = ClassId.fromString("org/babyfish/jimmer/Formula")
    val Transient = ClassId.fromString("org/babyfish/jimmer/sql/Transient")
    val ExcludeFromAllScalars = ClassId.fromString("org/babyfish/jimmer/sql/ExcludeFromAllScalars")
    val LogicalDeleted = ClassId.fromString("org/babyfish/jimmer/sql/LogicalDeleted")
    val ManyToManyView = ClassId.fromString("org/babyfish/jimmer/sql/ManyToManyView")
}