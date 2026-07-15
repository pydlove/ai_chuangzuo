package com.aichuangzuo.admin.modules.homebanner.service.impl;

import com.aichuangzuo.admin.modules.homebanner.dto.request.HomeBannerReq;
import com.aichuangzuo.admin.modules.homebanner.entity.HomeBannerEntity;
import com.aichuangzuo.admin.modules.homebanner.exception.HomeBannerErrorCode;
import com.aichuangzuo.admin.modules.homebanner.mapper.HomeBannerMapper;
import com.aichuangzuo.admin.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.admin.modules.homebanner.vo.HomeBannerVO;
import com.aichuangzuo.shared.exception.BusinessException;
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

    @Override
    public Long create(HomeBannerReq req) {
        HomeBannerEntity e = new HomeBannerEntity();
        e.setImageUrl(req.getImageUrl());
        e.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, HomeBannerReq req) {
        HomeBannerEntity e = requireExisting(id);
        e.setImageUrl(req.getImageUrl());
        e.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.updateById(e);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        mapper.deleteById(id);
    }

    private HomeBannerEntity requireExisting(Long id) {
        HomeBannerEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(HomeBannerErrorCode.BANNER_NOT_FOUND);
        }
        return e;
    }

    private HomeBannerVO toVo(HomeBannerEntity e) {
        HomeBannerVO v = new HomeBannerVO();
        v.setId(e.getId());
        v.setImageUrl(e.getImageUrl());
        v.setLinkUrl(e.getLinkUrl());
        v.setSort(e.getSort());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
