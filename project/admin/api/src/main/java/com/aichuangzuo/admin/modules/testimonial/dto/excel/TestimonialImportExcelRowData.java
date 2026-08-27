package com.aichuangzuo.admin.modules.testimonial.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TestimonialImportExcelRowData {

    @ExcelProperty("头像 URL（选填）")
    private String avatarUrl;

    @ExcelProperty("姓名（必填）")
    private String name;

    @ExcelProperty("身份/职位（选填）")
    private String title;

    @ExcelProperty("星级（必填，1-5）")
    private String starRating;

    @ExcelProperty("评价内容（必填）")
    private String reviewText;

    @ExcelProperty("排序（选填，默认 0）")
    private String sort;

    @ExcelProperty("启用状态（选填，0=禁用，1=启用；默认 1）")
    private String isEnabled;
}
