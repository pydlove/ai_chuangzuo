package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class HotSearchConfigServiceTest {

    @Autowired
    private HotSearchConfigService configService;

    @Test
    void shouldReadDefaultConfig() {
        assertNotNull(configService.getConfig());
    }

    @Test
    void shouldRejectInvalidCron() {
        HotSearchConfigRequest req = new HotSearchConfigRequest();
        req.setCron("not-a-cron");
        req.setEnabled(1);
        req.setTopN(50);
        req.setConnectTimeoutMillis(5000);
        req.setReadTimeoutMillis(10000);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> configService.saveConfig(req, 1L));
        assertEquals(AdminHotSearchErrorCode.INVALID_CRON.getCode(), ex.getCode());
    }

    @Test
    void shouldSaveValidConfig() {
        HotSearchConfigRequest req = new HotSearchConfigRequest();
        req.setCron("0 0 3 * * ?");
        req.setEnabled(1);
        req.setTopN(30);
        req.setConnectTimeoutMillis(5000);
        req.setReadTimeoutMillis(10000);
        configService.saveConfig(req, 1L);
        assertEquals("0 0 3 * * ?", configService.getConfig().getCron());
    }
}
