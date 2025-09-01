package com.back.domain.trade.entity

import com.back.domain.member.entity.Member
import com.back.domain.post.entity.Post
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Trade(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    var seller: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    var buyer: Member,

    @Column(nullable = false)
    var price: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TradeStatus
) : BaseEntity()
