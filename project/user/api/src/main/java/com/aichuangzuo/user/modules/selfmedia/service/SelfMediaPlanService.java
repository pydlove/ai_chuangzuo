package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SelfMediaPlanService {
    SelfMediaPlanVO getCurrentPlan(Long userId);
    SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request);
    List<QuestionVO> getOrGeneratePlatformQuestions(Long userId, String platformKey);
    List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request);
    RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request);

    /**
     * 运营方案库：匿名分页浏览全平台用户的运营方案。
     */
    IPage<SelfMediaPlanGalleryVO> galleryPage(int page, int pageSize);
}
