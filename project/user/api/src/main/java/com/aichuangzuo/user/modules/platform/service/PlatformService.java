package com.aichuangzuo.user.modules.platform.service;

import com.aichuangzuo.user.modules.platform.vo.PlatformVO;

import java.util.List;

/**
 * 用户端自媒体平台配置查询服务。
 */
public interface PlatformService {

    /**
     * 查询启用的平台列表，按排序号升序。
     */
    List<PlatformVO> listEnabled();
}
