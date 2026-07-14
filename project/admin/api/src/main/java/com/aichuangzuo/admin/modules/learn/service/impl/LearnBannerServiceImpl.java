package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnBannerEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnBannerMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnBannerService;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnBannerServiceImpl implements LearnBannerService {

    private final LearnBannerMapper mapper;

    @Override
    public List<LearnBannerVO> list() {
        return mapper.selectList(new QueryWrapper<LearnBannerEntity>().orderByAsc("sort"))
                .stream().map(this::toVo).toList();
    }

    @Override
    public Long create(LearnBannerReq req) {
        LearnBannerEntity e = new LearnBannerEntity();
        e.setImageUrl(req.getImageUrl());
        e.setLinkUrl(req.getLinkUrl() != null ? req.getLinkUrl() : "");
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, LearnBannerReq req) {
        LearnBannerEntity e = requireExisting(id);
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

    private LearnBannerEntity requireExisting(Long id) {
        LearnBannerEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(LearnErrorCode.BANNER_NOT_FOUND);
        }
        return e;
    }

    private LearnBannerVO toVo(LearnBannerEntity e) {
        LearnBannerVO v = new LearnBannerVO();
        v.setId(e.getId());
        v.setImageUrl(e.getImageUrl());
        v.setLinkUrl(e.getLinkUrl());
        v.setSort(e.getSort());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
