package com.back.domain.post.entity

import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.files.files.entity.Files
import com.back.domain.member.entity.Member
import com.back.domain.trade.entity.Trade
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Post : BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    lateinit var member: Member
        private set

    @Column(nullable = false)
    lateinit var title: String
        private set

    @Column(columnDefinition = "TEXT", nullable = false)
    lateinit var description: String
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var category: Category
        private set

    @Column(nullable = false)
    var price: Int = 0
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var status: Status
        private set

    @Column(name = "favorite_cnt", nullable = false)
    var favoriteCnt: Int = 0
        private set

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatRooms: MutableList<ChatRoom> = mutableListOf()

    @OneToOne(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val trade: Trade? = null

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val postFiles: MutableList<Files> = mutableListOf()

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val favoritePosts: MutableList<FavoritePost> = mutableListOf()

    protected constructor()

    constructor(
        member: Member, title: String?, description: String?,
        category: Category?, price: Int?, status: Status?
    ) {
        this.member = member
        this.title = title ?: ""
        this.description = description ?: ""
        this.category = category ?: Category.ETC
        this.price = price ?: 0
        this.status = status ?: Status.SALE
        this.favoriteCnt = 0
    }

    @PrePersist
    protected fun onCreate() {
        this.favoriteCnt = 0
    }

    enum class Category(val label: String) {
        PRODUCT("물건발명"),
        METHOD("방법발명"),
        USE("용도발명"),
        DESIGN("디자인권"),
        TRADEMARK("상표권"),
        COPYRIGHT("저작권"),
        ETC("기타");

        companion object {
            fun from(name: String?): Category? {
                return entries.find { it.name.equals(name, ignoreCase = true) }
            }
        }
    }

    enum class Status(val label: String) {
        SALE("판매중"),
        SOLD_OUT("판매완료"),
        SUSPENDED("판매중단")
    }

    fun markAsSoldOut() {
        if (this.status == Status.SOLD_OUT) {
            throw ServiceException("400-3", "이미 판매 완료된 게시글입니다.")
        }
        this.status = Status.SOLD_OUT
    }

    fun updatePost(title: String?, description: String?, category: Category?, price: Int) {
        title?.let { this.title = it }
        description?.let { this.description = it }
        category?.let { this.category = it }
        this.price = price
    }

    fun updateStatus(status: Status?) {
        status?.let { this.status = it }
    }

    companion object {
        fun stub(): Post {
            return Post(
                Member("stub", "stub", "stub", ""),
                "stub",
                "stub",
                Category.ETC,
                0,
                Status.SALE
            )
        }
    }
}
