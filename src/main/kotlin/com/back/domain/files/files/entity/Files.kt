package com.back.domain.files.files.entity

import com.back.domain.post.entity.Post
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "files")
class Files(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val fileType: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val fileUrl: String,

    @Column(nullable = false)
    val sortOrder: Int
) : BaseEntity() {

    // JPA를 위한 기본 생성자 (protected)
    protected constructor() : this(
        post = Post(), // 임시 객체, 실제로는 JPA가 처리
        fileName = "",
        fileType = "",
        fileSize = 0L,
        fileUrl = "",
        sortOrder = 0
    )
}