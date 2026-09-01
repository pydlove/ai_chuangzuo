package com.aichuangzuo.admin.modules.experience.dto.request;

import lombok.Data;

@Data
public class ExperienceTokenQueryRequest {

    private String batchId;
    private Integer status;
    private Long page = 1L;
    private Long size = 20L;
}
