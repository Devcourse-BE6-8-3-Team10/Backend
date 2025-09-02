package com.back.global.jpa.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0L  // 0L로 초기화 후 JPA가 실제값으로 변경

    @CreatedDate
    open lateinit var createdAt: LocalDateTime  // non-null이지만 JPA가 초기화

    @LastModifiedDate
    open lateinit var modifiedAt: LocalDateTime
}
