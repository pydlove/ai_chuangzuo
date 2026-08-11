package com.aichuangzuo.user.modules.share.service.impl;

import com.aichuangzuo.shared.entity.ShareConfig;
import com.aichuangzuo.user.modules.share.mapper.ShareConfigMapper;
import com.aichuangzuo.user.modules.share.service.ShareConfigService;
import com.aichuangzuo.user.modules.share.vo.ShareConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareConfigServiceImpl implements ShareConfigService {

    private final ShareConfigMapper shareConfigMapper;

    @Override
    public ShareConfigVO getEnabledBySceneKey(String sceneKey) {
        ShareConfig config = shareConfigMapper.selectEnabledBySceneKey(sceneKey);
        if (config == null) {
            log.warn("分享配置未找到, sceneKey={}", sceneKey);
            return null;
        }
        ShareConfigVO vo = new ShareConfigVO();
        vo.setId(config.getId());
        vo.setSceneKey(config.getSceneKey());
        vo.setTitle(config.getTitle());
        vo.setContent(config.getContent());
        return vo;
    }
}
