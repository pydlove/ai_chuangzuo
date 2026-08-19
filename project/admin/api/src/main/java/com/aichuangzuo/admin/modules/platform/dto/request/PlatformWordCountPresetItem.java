 package com.aichuangzuo.admin.modules.platform.dto.request;
 
 import jakarta.validation.constraints.Min;
 import jakarta.validation.constraints.NotBlank;
 import jakarta.validation.constraints.Size;
 import lombok.Data;
 
 /**
  * 平台字数档位配置项。
  */
 @Data
 public class PlatformWordCountPresetItem {
 
     @NotBlank(message = "字数档位标签不能为空")
     @Size(max = 64, message = "字数档位标签长度不能超过 64")
     private String label;
 
     @Min(value = 1, message = "字数必须大于 0")
     private Integer count;
 }
