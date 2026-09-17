package com.aichuangzuo.admin.modules.simulation.util;

import com.aichuangzuo.admin.modules.simulation.dto.excel.SimulationNicknameExcelRowData;
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

/**
 * 模拟运营-昵称库 Excel 导入：单列"昵称"，逐行读取。
 */
public final class SimulationNicknameExcelUtil {

    private static final String TEMPLATE_SHEET_NAME = "昵称导入模板";
    private static final String EXTENSION = ".xlsx";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> EXPECTED_HEADERS = List.of("昵称");

    private SimulationNicknameExcelUtil() {
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream, SimulationNicknameExcelRowData.class)
                .sheet(TEMPLATE_SHEET_NAME)
                .doWrite(List.of(example("晚风轻轻吹"), example("山间的雾")));
    }

    public static List<String> readNicknames(MultipartFile file) {
        validateFile(file);
        NicknameListener listener = new NicknameListener();
        try {
            EasyExcel.read(file.getInputStream(), SimulationNicknameExcelRowData.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_PARSE_ERROR);
        }
        List<String> rows = listener.getNicknames();
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

    private static SimulationNicknameExcelRowData example(String nickname) {
        SimulationNicknameExcelRowData row = new SimulationNicknameExcelRowData();
        row.setNickname(nickname);
        return row;
    }

    private static class NicknameListener extends AnalysisEventListener<SimulationNicknameExcelRowData> {
        private final List<String> nicknames = new ArrayList<>();
        private boolean headValidated = false;

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (headValidated) {
                return;
            }
            headValidated = true;
            String actual = headMap.getOrDefault(0, "").trim();
            if (!EXPECTED_HEADERS.get(0).equals(actual)) {
                throw new BusinessException(AdminUserErrorCode.EXCEL_FILE_INVALID);
            }
        }

        @Override
        public void invoke(SimulationNicknameExcelRowData row, AnalysisContext context) {
            if (row == null || row.getNickname() == null || row.getNickname().trim().isEmpty()) {
                return;
            }
            nicknames.add(row.getNickname().trim());
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // no-op
        }

        public List<String> getNicknames() {
            return nicknames;
        }
    }
}
