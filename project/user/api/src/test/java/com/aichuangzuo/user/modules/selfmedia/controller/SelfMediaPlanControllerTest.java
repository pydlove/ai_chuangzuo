package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.SavePlanRequest;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfMediaPlanControllerTest {

    @Mock
    private SelfMediaPlanService planService;

    @InjectMocks
    private SelfMediaPlanController controller;

    @BeforeEach
    void setUp() {
        SecurityUserContext.setCurrentUserId(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityUserContext.clear();
    }

    @Test
    void getCurrentPlan_shouldReturnServiceResult() {
        SelfMediaPlanVO vo = new SelfMediaPlanVO();
        vo.setPlatformKey("xiaohongshu");
        when(planService.getCurrentPlan(1L)).thenReturn(vo);

        Result<SelfMediaPlanVO> result = controller.getCurrentPlan();

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("xiaohongshu", result.getData().getPlatformKey());
    }

    @Test
    void savePlan_shouldCallServiceAndReturnVo() {
        SavePlanRequest req = new SavePlanRequest();
        req.setPlatformKey("xiaohongshu");
        req.setNicheKey("zhichangzhuanxing");
        req.setPersonaKey("experiencer");

        SelfMediaPlanVO vo = new SelfMediaPlanVO();
        vo.setPlatformKey("xiaohongshu");
        when(planService.savePlan(eq(1L), any(SavePlanRequest.class))).thenReturn(vo);

        Result<SelfMediaPlanVO> result = controller.savePlan(req);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        verify(planService).savePlan(eq(1L), any(SavePlanRequest.class));
    }
}
