package com.aichuangzuo.admin.modules.simulation.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 昵称库 Excel 导入行：单列"昵称"。
 */
@Data
public class SimulationNicknameExcelRowData {

    @ExcelProperty("昵称")
    private String nickname;
}
