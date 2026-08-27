package com.aichuangzuo.admin.modules.testimonial.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialImportResultVO {

    private boolean success;
    private int totalRows;
    private int importedCount;
    private List<TestimonialImportRowErrorVO> errors;
}
