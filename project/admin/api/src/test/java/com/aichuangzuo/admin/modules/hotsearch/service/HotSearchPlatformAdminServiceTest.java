package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
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
class HotSearchPlatformAdminServiceTest {

    @Autowired
    private HotSearchPlatformAdminService service;

    @Test
    void shouldCreateAndDelete() {
        HotSearchPlatformRequest req = new HotSearchPlatformRequest();
        req.setCode("test_plat_" + System.currentTimeMillis());
        req.setName("Test");
        req.setEnabled(1);
        var created = service.create(req);
        assertNotNull(created.getId());
        service.delete(created.getId());
    }

    @Test
    void shouldRejectDuplicateCode() {
        HotSearchPlatformRequest req = new HotSearchPlatformRequest();
        req.setCode("douyin"); // 已存在
        req.setName("Dup");
        req.setEnabled(1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminHotSearchErrorCode.PLATFORM_CODE_DUPLICATED.getCode(), ex.getCode());
    }
}
