package com.back.domain.files.files.repository;

import com.back.domain.files.files.entity.Files;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {
    List<Files> findByPostIdOrderBySortOrderAsc(Long postId);
    Optional<Files> findById(Long fileId);

    @Query("SELECT f FROM Files f JOIN FETCH f.post WHERE f.post.id = :postId ORDER BY f.sortOrder ASC")
    List<Files> findWithPostByPostId(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Files f WHERE f.post.id = :postId ORDER BY f.sortOrder DESC")
    List<Files> findLastByPostIdWithLock(@Param("postId") Long postId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"post"})
    Page<Files> findAll(Pageable pageable);
}
