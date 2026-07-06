package com.aichuangzuo.user.modules.hotsearch.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.enums.HotSearchErrorCode;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.user.modules.hotsearch.service.HotSearchService;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 热搜查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class HotSearchServiceImpl implements HotSearchService {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;

    @Override
    public List<HotSearchPlatformVO> listPlatforms() {
        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        return platformMapper.selectList(wrapper).stream()
                .map(this::toPlatformVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HotSearchItemVO> listByPlatformAndDate(String platformCode, LocalDate date) {
        HotSearchPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>()
                        .eq(HotSearchPlatform::getCode, platformCode));
        if (platform == null) {
            throw new BusinessException(HotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        if (platform.getEnabled() == null || platform.getEnabled() != 1) {
            throw new BusinessException(HotSearchErrorCode.PLATFORM_DISABLED);
        }

        LocalDate queryDate = date == null ? LocalDate.now() : date;
        List<HotSearchDaily> list = dailyMapper.selectByPlatformAndDate(platformCode, queryDate);
        return list.stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());
    }

    private HotSearchPlatformVO toPlatformVO(HotSearchPlatform platform) {
        HotSearchPlatformVO vo = new HotSearchPlatformVO();
        vo.setCode(platform.getCode());
        vo.setName(platform.getName());
        vo.setIcon(platform.getIcon());
        vo.setSortOrder(platform.getSortOrder());
        return vo;
    }

    private HotSearchItemVO toItemVO(HotSearchDaily daily) {
        HotSearchItemVO vo = new HotSearchItemVO();
        vo.setRank(daily.getRankNum());
        vo.setTitle(daily.getTitle());
        vo.setHotValue(daily.getHotValue());
        vo.setUrl(daily.getUrl());
        vo.setSearchCount(daily.getSearchCount());
        return vo;
    }
}
