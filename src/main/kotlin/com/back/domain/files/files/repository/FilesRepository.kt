package com.back.domain.files.files.repository

import com.back.domain.files.files.entity.Files
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FilesRepository : JpaRepository<Files, Long> {
    fun findByPostIdOrderBySortOrderAsc(postId: Long): List<Files>

    override fun findById(fileId: Long): Optional<Files>

    @Query("SELECT f FROM Files f JOIN FETCH f.post WHERE f.post.id = :postId ORDER BY f.sortOrder ASC")
    fun findWithPostByPostId(@Param("postId") postId: Long): List<Files>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Files f WHERE f.post.id = :postId ORDER BY f.sortOrder DESC")
    fun findLastByPostIdWithLock(@Param("postId") postId: Long, pageable: Pageable): List<Files>

    @EntityGraph(attributePaths = ["post"])
    override fun findAll(pageable: Pageable): Page<Files>
}