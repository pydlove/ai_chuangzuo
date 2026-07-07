package com.aichuangzuo.admin.infrastructure.ai;

import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;

import java.util.List;

public interface AiProviderClient {

    boolean testConnection(String baseUrl, String apiKey);

    List<ModelOptionVO> fetchModels(String baseUrl, String apiKey);

    ModelConfigChatTestVO chatTest(String baseUrl, String apiKey, String modelCode,
                                   String prompt, boolean stream);
}
