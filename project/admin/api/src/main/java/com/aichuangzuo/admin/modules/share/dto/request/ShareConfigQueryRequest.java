package com.aichuangzuo.admin.modules.share.dto.request;

import lombok.Data;

@Data
public class ShareConfigQueryRequest {

    private String sceneKey;
    private Integer enabled;
    private Long page = 1L;
    private Long size = 20L;
}
