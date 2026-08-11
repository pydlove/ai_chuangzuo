package com.aichuangzuo.admin.modules.share.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareConfigAdminVO {

    private Long id;
    private String sceneKey;
    private String title;
    private String content;
    private Integer enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
