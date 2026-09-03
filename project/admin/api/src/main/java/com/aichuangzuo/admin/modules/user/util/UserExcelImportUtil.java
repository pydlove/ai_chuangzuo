package com.aichuangzuo.admin.modules.user.util;

import com.aichuangzuo.admin.modules.user.dto.excel.UserImportExcelRowData;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
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

public final class UserExcelImportUtil {

    private static final String TEMPLATE_SHEET_NAME = "用户导入模板";
    private static final String EXTENSION = ".xlsx";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "邮箱（选填，与手机号二选一）", "手机号（选填，与邮箱二选一）", "昵称（必填）", "密码（选填，留空默认 Aichuangzuo@123）", "用户类型（选填，0=机器人，1=真实用户；默认 0）"
    );
    private static final List<UserImportExcelRowData> TEMPLATE_EXAMPLES = List.of(
            createExample("robot01@example.com", "", "机器人一号", "", "0"),
            createExample("", "13800138000", "机器人二号", "Aichuangzuo@123", "0")
    );

    private UserExcelImportUtil() {
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream, UserImportExcelRowData.class)
                .sheet(TEMPLATE_SHEET_NAME)
                .doWrite(TEMPLATE_EXAMPLES);
    }

    public static List<UserImportExcelRowData> readRows(MultipartFile file) {
        validateFile(file);
        UserImportListener listener = new UserImportListener();
        try {
            EasyExcel.read(file.getInputStream(), UserImportExcelRowData.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_PARSE_ERROR);
        }
        List<UserImportExcelRowData> rows = listener.getRows();
        if (rows.isEmpty()) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_IMPORT_EMPTY);
        }
        return rows;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_FILE_INVALID);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(EXTENSION)) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_FILE_INVALID);
        }
    }

    private static UserImportExcelRowData createExample(String email, String phone, String nickname, String password, String userType) {
        UserImportExcelRowData example = new UserImportExcelRowData();
        example.setEmail(email);
        example.setPhone(phone);
        example.setNickname(nickname);
        example.setPassword(password);
        example.setUserType(userType);
        return example;
    }

    private static class UserImportListener extends AnalysisEventListener<UserImportExcelRowData> {
        private final List<UserImportExcelRowData> rows = new ArrayList<>();
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
                throw new BusinessException(AdminUserErrorCode.EXCEL_FILE_INVALID);
            }
        }

        @Override
        public void invoke(UserImportExcelRowData row, AnalysisContext context) {
            if (isBlankRow(row)) {
                return;
            }
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // no-op
        }

        public List<UserImportExcelRowData> getRows() {
            return rows;
        }

        private boolean isBlankRow(UserImportExcelRowData row) {
            return isBlank(row.getEmail())
                    && isBlank(row.getPhone())
                    && isBlank(row.getNickname())
                    && isBlank(row.getPassword())
                    && isBlank(row.getUserType());
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
