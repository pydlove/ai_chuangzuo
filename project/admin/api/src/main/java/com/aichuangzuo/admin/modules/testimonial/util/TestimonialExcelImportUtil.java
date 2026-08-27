package com.aichuangzuo.admin.modules.testimonial.util;

import com.aichuangzuo.admin.modules.testimonial.dto.excel.TestimonialImportExcelRowData;
import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TestimonialExcelImportUtil {

    private static final String TEMPLATE_SHEET_NAME = "评价导入模板";
    private static final String EXTENSION = ".xlsx";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "头像 URL（选填）", "姓名（必填）", "身份/职位（选填）", "星级（必填，1-5）",
            "评价内容（必填）", "排序（选填，默认 0）", "启用状态（选填，0=禁用，1=启用；默认 1）"
    );
    private static final List<TestimonialImportExcelRowData> TEMPLATE_EXAMPLES = List.of(
            createExample("", "张明", "计算机专业学生", "5", "爱创作帮我快速完成了课程作业，非常省心！", "0", "1"),
            createExample("https://example.com/avatar.png", "李华", "自媒体运营", "4", "生成的小红书文案很贴合热点，省了不少时间。", "1", "1")
    );

    private TestimonialExcelImportUtil() {
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream, TestimonialImportExcelRowData.class)
                .sheet(TEMPLATE_SHEET_NAME)
                .doWrite(TEMPLATE_EXAMPLES);
    }

    public static List<TestimonialImportExcelRowData> readRows(MultipartFile file) {
        validateFile(file);
        TestimonialImportListener listener = new TestimonialImportListener();
        try {
            EasyExcel.read(file.getInputStream(), TestimonialImportExcelRowData.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_PARSE_ERROR);
        }
        List<TestimonialImportExcelRowData> rows = listener.getRows();
        if (rows.isEmpty()) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_IMPORT_EMPTY);
        }
        return rows;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_FILE_INVALID);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(EXTENSION)) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_FILE_INVALID);
        }
    }

    private static TestimonialImportExcelRowData createExample(String avatarUrl, String name, String title,
                                                               String starRating, String reviewText,
                                                               String sort, String isEnabled) {
        TestimonialImportExcelRowData example = new TestimonialImportExcelRowData();
        example.setAvatarUrl(avatarUrl);
        example.setName(name);
        example.setTitle(title);
        example.setStarRating(starRating);
        example.setReviewText(reviewText);
        example.setSort(sort);
        example.setIsEnabled(isEnabled);
        return example;
    }

    private static class TestimonialImportListener extends AnalysisEventListener<TestimonialImportExcelRowData> {
        private final List<TestimonialImportExcelRowData> rows = new ArrayList<>();
        private boolean headValidated = false;

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (headValidated) {
                return;
            }
            headValidated = true;
            List<String> actualHeaders = new ArrayList<>();
            for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
                actualHeaders.add(headMap.getOrDefault(i, "").trim());
            }
            if (!EXPECTED_HEADERS.equals(actualHeaders)) {
                throw new BusinessException(TestimonialErrorCode.EXCEL_FILE_INVALID);
            }
        }

        @Override
        public void invoke(TestimonialImportExcelRowData row, AnalysisContext context) {
            if (isBlankRow(row)) {
                return;
            }
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // no-op
        }

        public List<TestimonialImportExcelRowData> getRows() {
            return rows;
        }

        private boolean isBlankRow(TestimonialImportExcelRowData row) {
            return isBlank(row.getName())
                    && isBlank(row.getStarRating())
                    && isBlank(row.getReviewText())
                    && isBlank(row.getTitle())
                    && isBlank(row.getAvatarUrl())
                    && isBlank(row.getSort())
                    && isBlank(row.getIsEnabled());
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
