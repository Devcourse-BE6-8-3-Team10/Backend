package com.back.domain.files.files.entity;

import com.back.domain.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

//    public String getFileUrl() {
//        return fileUrl;
//    }
    // 정렬 순서
    @Column(nullable = false)
    private int sortOrder;

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
}


