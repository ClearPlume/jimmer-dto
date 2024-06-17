package net.fallingangel.jimmerdto.entity.common

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface TenantAware {
    val tenant: String
}