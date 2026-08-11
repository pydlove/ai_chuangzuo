package com.aichuangzuo.user.modules.share.service;

import com.aichuangzuo.user.modules.share.vo.ShareConfigVO;

public interface ShareConfigService {

    ShareConfigVO getEnabledBySceneKey(String sceneKey);
}
