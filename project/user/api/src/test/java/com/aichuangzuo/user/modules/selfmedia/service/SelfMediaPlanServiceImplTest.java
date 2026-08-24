package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
import com.aichuangzuo.user.modules.selfmedia.dto.request.SavePlanRequest;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanNicheMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanPersonaMapper;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanQuestionMapper;
import com.aichuangzuo.user.modules.selfmedia.service.impl.SelfMediaPlanServiceImpl;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SelfMediaPlanServiceImplTest {

    private final SelfMediaPlanAiService aiService = mock(SelfMediaPlanAiService.class);
    private final SelfMediaPlanMapper planMapper = mock(SelfMediaPlanMapper.class);
    private final SelfMediaPlanQuestionMapper questionMapper = mock(SelfMediaPlanQuestionMapper.class);
    private final SelfMediaPlanNicheMapper nicheMapper = mock(SelfMediaPlanNicheMapper.class);
    private final SelfMediaPlanPersonaMapper personaMapper = mock(SelfMediaPlanPersonaMapper.class);
    private final PlatformMapper platformMapper = mock(PlatformMapper.class);
    private final BenefitService benefitService = mock(BenefitService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SelfMediaPlanServiceImpl service() {
        return new SelfMediaPlanServiceImpl(aiService, planMapper, questionMapper, nicheMapper, personaMapper, platformMapper, benefitService, objectMapper);
    }

    @Test
    void savePlan_shouldInsertAndReturnVo() {
        SavePlanRequest req = new SavePlanRequest();
        req.setPlatformKey("xiaohongshu");
        req.setPlatformName("小红书");
        req.setNicheKey("zhichangzhuanxing");
        req.setNicheName("35+ 职场转型");
        req.setPersonaKey("experiencer");
        req.setPersonaName("实战记录者");
        PillarVO p = new PillarVO();
        p.setName("干货复盘");
        p.setPercent(60);
        req.setPillars(List.of(p));

        when(planMapper.selectByUserId(1L)).thenReturn(null);

        service().savePlan(1L, req);

        ArgumentCaptor<SelfMediaPlan> captor = ArgumentCaptor.forClass(SelfMediaPlan.class);
        verify(planMapper).insert(captor.capture());
        SelfMediaPlan inserted = captor.getValue();

        assertEquals(1L, inserted.getUserId());
        assertEquals("xiaohongshu", inserted.getPlatformKey());
        assertEquals("小红书", inserted.getPlatformName());
        assertEquals("zhichangzhuanxing", inserted.getNicheKey());
        assertEquals("experiencer", inserted.getPersonaKey());
        assertNotNull(inserted.getContentPillarsJson());
    }
}
