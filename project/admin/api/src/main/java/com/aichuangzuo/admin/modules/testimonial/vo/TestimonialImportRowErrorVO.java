package com.aichuangzuo.admin.modules.testimonial.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialImportRowErrorVO {

    private Integer rowIndex;
    private String name;
    private List<String> errors;
}
