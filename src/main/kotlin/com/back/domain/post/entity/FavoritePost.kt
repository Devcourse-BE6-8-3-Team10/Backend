package com.back.domain.post.entity

import com.back.domain.member.entity.Member
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "post_id"])])
class FavoritePost(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post
) : BaseEntity()
