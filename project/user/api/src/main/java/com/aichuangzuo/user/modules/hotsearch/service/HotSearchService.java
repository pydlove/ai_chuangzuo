package com.aichuangzuo.user.modules.hotsearch.service;

import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 热搜查询服务。
 */
public interface HotSearchService {

    /**
     * 获取启用的热搜平台列表。
     */
    List<HotSearchPlatformVO> listPlatforms();

    /**
     * 查询指定平台和日期的热搜榜单。
     *
     * @param platformCode 平台编码
     * @param date         快照日期
     * @return 热搜项列表，按排名升序
     */
    List<HotSearchItemVO> listByPlatformAndDate(String platformCode, LocalDate date);
}
