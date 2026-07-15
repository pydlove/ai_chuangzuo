package com.aichuangzuo.user.modules.homebanner.service.impl;

import com.aichuangzuo.user.modules.homebanner.entity.HomeBannerEntity;
import com.aichuangzuo.user.modules.homebanner.mapper.HomeBannerMapper;
import com.aichuangzuo.user.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.user.modules.homebanner.vo.HomeBannerVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {

    private final HomeBannerMapper mapper;

    @Override
    public List<HomeBannerVO> list() {
        return mapper.selectList(new QueryWrapper<HomeBannerEntity>().orderByAsc("sort"))
                .stream().map(this::toVo).toList();
    }

    private HomeBannerVO toVo(HomeBannerEntity e) {
        HomeBannerVO v = new HomeBannerVO();
        v.setId(e.getId());
        v.setImageUrl(e.getImageUrl());
        v.setLinkUrl(e.getLinkUrl());
        return v;
    }
}
