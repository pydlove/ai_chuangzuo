package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.SettlementRequest;
import com.aichuangzuo.admin.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.admin.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.admin.modules.earnings.vo.SettlementResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class SettlementAdminServiceTest {

    @Autowired
    private SettlementAdminService settlementAdminService;

    @Autowired
    private EarningsRecordMapper earningsRecordMapper;

    @Test
    void settle_updatesStatus() {
        EarningsRecord record = new EarningsRecord();
        record.setUserId(1L);
        record.setType("USAGE");
        record.setTitle("test");
        record.setAmount(new BigDecimal("10.00"));
        record.setStatus(0);
        record.setSettlementMonth("2026-06");
        earningsRecordMapper.insert(record);

        SettlementRequest request = new SettlementRequest();
        request.setMonth("2026-06");
        SettlementResultVO result = settlementAdminService.settle(request);

        assertTrue(result.getSettledRecordCount() >= 1);
        EarningsRecord updated = earningsRecordMapper.selectById(record.getId());
        assertEquals(1, updated.getStatus());
    }
}
