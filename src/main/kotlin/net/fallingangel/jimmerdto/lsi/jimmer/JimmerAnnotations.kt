package net.fallingangel.jimmerdto.lsi.jimmer

import net.fallingangel.jimmerdto.lsi.LName

@Suppress("unused")
object JimmerAnnotations {
    // org.babyfish.jimmer
    val ClientException = LName.fromFqn("org.babyfish.jimmer.ClientException")
    val Formula = LName.fromFqn("org.babyfish.jimmer.Formula")
    val Immutable = LName.fromFqn("org.babyfish.jimmer.Immutable")
    val Scalar = LName.fromFqn("org.babyfish.jimmer.Scalar")

    // org.babyfish.jimmer.client
    val ApiIgnore = LName.fromFqn("org.babyfish.jimmer.client.ApiIgnore")
    val Description = LName.fromFqn("org.babyfish.jimmer.client.Description")
    val EnableImplicitApi = LName.fromFqn("org.babyfish.jimmer.client.EnableImplicitApi")
    val ExportDoc = LName.fromFqn("org.babyfish.jimmer.client.ExportDoc")
    val FetchBy = LName.fromFqn("org.babyfish.jimmer.client.FetchBy")
    val TNullable = LName.fromFqn("org.babyfish.jimmer.client.TNullable")

    // org.babyfish.jimmer.client.meta
    val Api = LName.fromFqn("org.babyfish.jimmer.client.meta.Api")
    val DefaultFetcherOwner = LName.fromFqn("org.babyfish.jimmer.client.meta.DefaultFetcherOwner")

    // org.babyfish.jimmer.error
    val ErrorFamily = LName.fromFqn("org.babyfish.jimmer.error.ErrorFamily")
    val ErrorField = LName.fromFqn("org.babyfish.jimmer.error.ErrorField")
    val ErrorFields = LName.fromFqn("org.babyfish.jimmer.error.ErrorFields")
    val GeneratedBy = LName.fromFqn("org.babyfish.jimmer.error.GeneratedBy")
    val GeneratedPolymorphicDtoBranch = LName.fromFqn("org.babyfish.jimmer.error.GeneratedPolymorphicDtoBranch")

    // org.babyfish.jimmer.internal
    val FixedInputField = LName.fromFqn("org.babyfish.jimmer.internal.FixedInputField")

    // org.babyfish.jimmer.jackson
    val JsonConverter = LName.fromFqn("org.babyfish.jimmer.jackson.JsonConverter")

    // org.babyfish.jimmer.kt
    val DslScope = LName.fromFqn("org.babyfish.jimmer.kt.DslScope")

    // org.babyfish.jimmer.kt.dto
    val KotlinDto = LName.fromFqn("org.babyfish.jimmer.kt.dto.KotlinDto")

    // org.babyfish.jimmer.lang
    val NewChain = LName.fromFqn("org.babyfish.jimmer.lang.NewChain")
    val OldChain = LName.fromFqn("org.babyfish.jimmer.lang.OldChain")

    // org.babyfish.jimmer.sql
    val Column = LName.fromFqn("org.babyfish.jimmer.sql.Column")
    val DatabaseDefault = LName.fromFqn("org.babyfish.jimmer.sql.DatabaseDefault")
    val DatabaseValidationIgnore = LName.fromFqn("org.babyfish.jimmer.sql.DatabaseValidationIgnore")
    val Default = LName.fromFqn("org.babyfish.jimmer.sql.Default")
    val Discriminator = LName.fromFqn("org.babyfish.jimmer.sql.Discriminator")
    val DiscriminatorValue = LName.fromFqn("org.babyfish.jimmer.sql.DiscriminatorValue")
    val Embeddable = LName.fromFqn("org.babyfish.jimmer.sql.Embeddable")
    val EnableDtoGeneration = LName.fromFqn("org.babyfish.jimmer.sql.EnableDtoGeneration")
    val Entity = LName.fromFqn("org.babyfish.jimmer.sql.Entity")
    val EnumItem = LName.fromFqn("org.babyfish.jimmer.sql.EnumItem")
    val ExcludeFromAllScalars = LName.fromFqn("org.babyfish.jimmer.sql.ExcludeFromAllScalars")
    val GeneratedValue = LName.fromFqn("org.babyfish.jimmer.sql.GeneratedValue")
    val Id = LName.fromFqn("org.babyfish.jimmer.sql.Id")
    val IdView = LName.fromFqn("org.babyfish.jimmer.sql.IdView")
    val Inheritance = LName.fromFqn("org.babyfish.jimmer.sql.Inheritance")
    val JoinColumn = LName.fromFqn("org.babyfish.jimmer.sql.JoinColumn")
    val JoinColumns = LName.fromFqn("org.babyfish.jimmer.sql.JoinColumns")
    val JoinSql = LName.fromFqn("org.babyfish.jimmer.sql.JoinSql")
    val JoinTable = LName.fromFqn("org.babyfish.jimmer.sql.JoinTable")
    val JoinTableFilter = LName("org.babyfish.jimmer.sql", listOf("JoinTable"), "JoinTableFilter")
    val LogicalDeletedFilter = LName("org.babyfish.jimmer.sql", listOf("JoinTable"), "LogicalDeletedFilter")
    val Key = LName.fromFqn("org.babyfish.jimmer.sql.Key")
    val Keys = LName.fromFqn("org.babyfish.jimmer.sql.Keys")
    val KeyUniqueConstraint = LName.fromFqn("org.babyfish.jimmer.sql.KeyUniqueConstraint")
    val LogicalDeleted = LName.fromFqn("org.babyfish.jimmer.sql.LogicalDeleted")
    val ManyToMany = LName.fromFqn("org.babyfish.jimmer.sql.ManyToMany")
    val ManyToManyView = LName.fromFqn("org.babyfish.jimmer.sql.ManyToManyView")
    val ManyToOne = LName.fromFqn("org.babyfish.jimmer.sql.ManyToOne")
    val MappedSuperclass = LName.fromFqn("org.babyfish.jimmer.sql.MappedSuperclass")
    val MapsId = LName.fromFqn("org.babyfish.jimmer.sql.MapsId")
    val OnDissociate = LName.fromFqn("org.babyfish.jimmer.sql.OnDissociate")
    val OneToMany = LName.fromFqn("org.babyfish.jimmer.sql.OneToMany")
    val OneToOne = LName.fromFqn("org.babyfish.jimmer.sql.OneToOne")
    val OrderedProp = LName.fromFqn("org.babyfish.jimmer.sql.OrderedProp")
    val PropOverride = LName.fromFqn("org.babyfish.jimmer.sql.PropOverride")
    val PropOverrides = LName.fromFqn("org.babyfish.jimmer.sql.PropOverrides")
    val Serialized = LName.fromFqn("org.babyfish.jimmer.sql.Serialized")
    val Table = LName.fromFqn("org.babyfish.jimmer.sql.Table")
    val Transient = LName.fromFqn("org.babyfish.jimmer.sql.Transient")
    val TypedTuple = LName.fromFqn("org.babyfish.jimmer.sql.TypedTuple")
    val Version = LName.fromFqn("org.babyfish.jimmer.sql.Version")

    // org.babyfish.jimmer.sql.ast.table
    val PropsFor = LName.fromFqn("org.babyfish.jimmer.sql.ast.table.PropsFor")

    // org.babyfish.jimmer.sql.ast.table.spi
    val UsingWeakJoinMetadataParser = LName.fromFqn("org.babyfish.jimmer.sql.ast.table.spi.UsingWeakJoinMetadataParser")

    // org.babyfish.jimmer.sql.transaction
    val TargetAnnotation = LName.fromFqn("org.babyfish.jimmer.sql.transaction.TargetAnnotation")
    val Tx = LName.fromFqn("org.babyfish.jimmer.sql.transaction.Tx")
}
