package net.fallingangel.jimmerdto.lsi.jimmer

import net.fallingangel.jimmerdto.lsi.LName

object JimmerAnnotations {
    val Entity = LName.fromFqn("org.babyfish.jimmer.sql.Entity")
    val MappedSuperclass = LName.fromFqn("org.babyfish.jimmer.sql.MappedSuperclass")
    val Embeddable = LName.fromFqn("org.babyfish.jimmer.sql.Embeddable")
    val Immutable = LName.fromFqn("org.babyfish.jimmer.Immutable")
    val TNullable = LName.fromFqn("org.babyfish.jimmer.client.TNullable")

    val Id = LName.fromFqn("org.babyfish.jimmer.sql.Id")
    val IdView = LName.fromFqn("org.babyfish.jimmer.sql.IdView")
    val Key = LName.fromFqn("org.babyfish.jimmer.sql.Key")
    val Formula = LName.fromFqn("org.babyfish.jimmer.Formula")
    val Transient = LName.fromFqn("org.babyfish.jimmer.sql.Transient")
    val ExcludeFromAllScalars = LName.fromFqn("org.babyfish.jimmer.sql.ExcludeFromAllScalars")
    val LogicalDeleted = LName.fromFqn("org.babyfish.jimmer.sql.LogicalDeleted")
    val ManyToManyView = LName.fromFqn("org.babyfish.jimmer.sql.ManyToManyView")
    val KotlinDto = LName.fromFqn("org.babyfish.jimmer.kt.dto.KotlinDto")
    val GeneratedValue = LName.fromFqn("org.babyfish.jimmer.sql.GeneratedValue")
}
