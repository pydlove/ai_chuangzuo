package com.aichuangzuo.admin.modules.hotsearch.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class HotSearchDailyQueryRequest {
    private String platform;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private Long page = 1L;
    private Long size = 20L;
}
