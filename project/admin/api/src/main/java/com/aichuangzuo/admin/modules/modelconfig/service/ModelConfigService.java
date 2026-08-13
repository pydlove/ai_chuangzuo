package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigChatTestRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;

import java.util.List;

public interface ModelConfigService {

    List<ModelConfigVO> listConfigs();

    ModelConfigVO getConfig(Long id);

    Long createConfig(ModelConfigSaveRequest request);

    void updateConfig(Long id, ModelConfigSaveRequest request);

    void deleteConfig(Long id);

    List<ModelOptionVO> fetchModels(ModelConfigConnectionRequest request);

    List<ModelOptionVO> listProviderModels(String providerType);

    boolean testConnection(ModelConfigConnectionRequest request);

    void toggleActive(Long id, ModelConfigActiveRequest request);

    ModelConfigChatTestVO chatTest(ModelConfigChatTestRequest request);
}
