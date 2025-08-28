package com.back.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdatePatentRequest(
    @NotBlank(message = "제목은 필수입니다.")
    String title,
    
    @NotBlank(message = "설명은 필수입니다.")
    String description,
    
    @NotBlank(message = "카테고리는 필수입니다.")
    String category,
    
    Integer price,
    
    @NotBlank(message = "상태는 필수입니다.")
    String status
) {} 