package net.fallingangel.jimmerdto.entity

import org.babyfish.jimmer.sql.*
import net.fallingangel.jimmerdto.entity.common.BaseEntity
import java.math.BigDecimal

@Entity
interface BookStore : BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key // ❶
    val name: String
    
    val website: String?

    @OneToMany(mappedBy = "store") // ❷
    val books: List<Book>

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The complex calculated properties are shown here.
    // As for the simple calculated properties, you can view `Author.fullName`
    // -----------------------------

    @Transient(ref = "bookStoreAvgPriceResolver") // ❸
    val avgPrice: BigDecimal

    /*
     * For example, if `BookStore.books` returns `[
     *     {name: A, edition: 1}, {name: A, edition: 2}, {name: A, edition: 3},
     *     {name: B, edition: 1}, {name: B, edition: 2}
     * ]`, `BookStore.newestBooks` returns `[
     *     {name: A, edition: 3}, {name: B, edition: 2}
     * ]`
     *
     * It is worth noting that if the calculated property returns entity object
     * or entity list, the shape can be controlled by the deeper child fetcher
     */
    @Transient(ref = "bookStoreNewestBooksResolver") // ❹
    val newestBooks: List<Book>
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❷ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/one-to-many
❸ ❹ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/calculated/transient
---------------------------------------------------*/
