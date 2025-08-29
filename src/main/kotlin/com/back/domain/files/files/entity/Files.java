package com.back.domain.files.files.entity;

import com.back.domain.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "files")
@EntityListeners(AuditingEntityListener.class)  // BaseEntity에서 가져온 부분
public class Files {

    // BaseEntity에서 가져온 필드들
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    // 기존 Files 필드들
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private int sortOrder;

    // BaseEntity에서 가져온 Getter 메서드들
    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    // 기존 Files Getter 메서드들
    public Post getPost() {
        return post;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    // 직접 생성자 (BaseEntity 필드 포함)
    public Files(Post post, String fileName, String fileType, long fileSize, String fileUrl, int sortOrder) {
        this.post = post;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.sortOrder = sortOrder;
    }
}