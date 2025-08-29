package com.back.domain.files.files.entity;

import com.back.domain.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "files")
public class Files extends BaseEntity {

    // 연관 게시글 참조
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

    // 정렬 순서
    @Column(nullable = false)
    private int sortOrder;

    // Getter 메서드
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


    public Files(Post post, String fileName, String fileType, long fileSize, String fileUrl, int sortOrder) {
        this.post = post;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.sortOrder = sortOrder;
    }
}


