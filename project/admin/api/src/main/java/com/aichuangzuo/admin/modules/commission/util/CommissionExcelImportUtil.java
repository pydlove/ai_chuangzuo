package com.aichuangzuo.admin.modules.commission.util;

import com.aichuangzuo.admin.modules.commission.dto.excel.CommissionTaskExcelRowData;
import com.aichuangzuo.admin.modules.commission.enums.AdminCommissionErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommissionExcelImportUtil {

    private static final String SHEET_NAME = "约稿任务数据";
    private static final String EXTENSION = ".xlsx";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "序号", "任务标题（必填）", "需求描述（必填）", "最小字数（必填）", "最大字数（必填）",
            "风格提示（选填）", "每篇奖励/创作币（必填）", "需采纳数量/篇（必填）",
            "投递截止时间（必填）", "评选截止时间（必填）"
    );

    private CommissionExcelImportUtil() {
    }

    public static List<CommissionTaskExcelRowData> readRows(MultipartFile file) {
        validateFile(file);
        CommissionTaskImportListener listener = new CommissionTaskImportListener();
        try {
            EasyExcel.read(file.getInputStream(), CommissionTaskExcelRowData.class, listener)
                    .sheet(SHEET_NAME)
                    .doRead();
        } catch (IOException e) {
            throw new BusinessException(AdminCommissionErrorCode.EXCEL_PARSE_ERROR);
        }
        List<CommissionTaskExcelRowData> rows = listener.getRows();
        if (rows.isEmpty()) {
            throw new BusinessException(AdminCommissionErrorCode.EXCEL_FILE_INVALID);
        }
        return rows;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AdminCommissionErrorCode.EXCEL_FILE_INVALID);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(EXTENSION)) {
            throw new BusinessException(AdminCommissionErrorCode.EXCEL_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(AdminCommissionErrorCode.EXCEL_FILE_INVALID);
        }
    }

    private static class CommissionTaskImportListener extends AnalysisEventListener<CommissionTaskExcelRowData> {
        private final List<CommissionTaskExcelRowData> rows = new ArrayList<>();
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
                throw new BusinessException(AdminCommissionErrorCode.EXCEL_FILE_INVALID);
            }
        }

        @Override
        public void invoke(CommissionTaskExcelRowData row, AnalysisContext context) {
            if (isBlankRow(row)) {
                return;
            }
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // no-op
        }

        public List<CommissionTaskExcelRowData> getRows() {
            return rows;
        }

        private boolean isBlankRow(CommissionTaskExcelRowData row) {
            return isBlank(row.getTitle())
                    && isBlank(row.getDescription())
                    && isBlank(row.getMinWordCount())
                    && isBlank(row.getMaxWordCount())
                    && isBlank(row.getSkillHint())
                    && isBlank(row.getRewardCoin())
                    && isBlank(row.getNeededCount())
                    && isBlank(row.getDeadlineAt())
                    && isBlank(row.getSelectionDeadlineAt());
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
