package com.aichuangzuo.user.modules.recommendedcreation.service;

import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.RecommendedCreationSessionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;

import java.util.List;

public interface RecommendedCreationService {

    RecommendedCreationSessionVO getSession(Long userId);

    List<TopicOptionVO> generateTopics(Long userId);

    List<AngleOptionVO> generateAngles(Long userId, String topicId);

    void updateSession(Long userId, UpdateSessionRequest request);

    GenerationTaskVO submitGeneration(Long userId);

    void clearSession(Long userId);
}
