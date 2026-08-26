package com.aichuangzuo.admin.modules.testimonial.service.impl;

import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialCreateRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialUpdateRequest;
import com.aichuangzuo.admin.modules.testimonial.entity.TestimonialEntity;
import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.admin.modules.testimonial.mapper.TestimonialMapper;
import com.aichuangzuo.admin.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialMapper mapper;

    @Override
    public List<TestimonialVO> list() {
        return mapper.selectList(
                        new QueryWrapper<TestimonialEntity>()
                                .orderByAsc("sort")
                                .orderByAsc("id")
                )
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public Long create(TestimonialCreateRequest req) {
        TestimonialEntity e = new TestimonialEntity();
        e.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        e.setName(req.getName());
        e.setTitle(req.getTitle() != null ? req.getTitle() : "");
        e.setStarRating(req.getStarRating());
        e.setReviewText(req.getReviewText());
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        e.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, TestimonialUpdateRequest req) {
        TestimonialEntity e = requireExisting(id);
        e.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        e.setName(req.getName());
        e.setTitle(req.getTitle() != null ? req.getTitle() : "");
        e.setStarRating(req.getStarRating());
        e.setReviewText(req.getReviewText());
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        e.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        mapper.updateById(e);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        mapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, TestimonialStatusRequest req) {
        TestimonialEntity e = requireExisting(id);
        e.setIsEnabled(req.getIsEnabled());
        mapper.updateById(e);
    }

    private TestimonialEntity requireExisting(Long id) {
        TestimonialEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(TestimonialErrorCode.TESTIMONIAL_NOT_FOUND);
        }
        return e;
    }

    private TestimonialVO toVo(TestimonialEntity e) {
        TestimonialVO v = new TestimonialVO();
        v.setId(e.getId());
        v.setAvatarUrl(e.getAvatarUrl());
        v.setName(e.getName());
        v.setTitle(e.getTitle());
        v.setStarRating(e.getStarRating());
        v.setReviewText(e.getReviewText());
        v.setSort(e.getSort());
        v.setIsEnabled(e.getIsEnabled());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
