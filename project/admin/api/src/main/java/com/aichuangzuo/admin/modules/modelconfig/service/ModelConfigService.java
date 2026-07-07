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

    ModelConfigVO getConfig(String providerType);

    void saveConfig(String providerType, ModelConfigSaveRequest request);

    void deleteConfig(String providerType);

    List<ModelOptionVO> fetchModels(String providerType, ModelConfigConnectionRequest request);

    boolean testConnection(String providerType, ModelConfigConnectionRequest request);

    void toggleActive(String providerType, ModelConfigActiveRequest request);

    ModelConfigChatTestVO chatTest(String providerType, ModelConfigChatTestRequest request);
}