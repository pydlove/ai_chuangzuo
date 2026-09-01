package com.aichuangzuo.user.modules.testimonial.service.impl;

import com.aichuangzuo.user.modules.testimonial.entity.TestimonialEntity;
import com.aichuangzuo.user.modules.testimonial.mapper.TestimonialMapper;
import com.aichuangzuo.user.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.user.modules.testimonial.vo.TestimonialVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialMapper mapper;

    @Override
    public List<TestimonialVO> listEnabled() {
        return mapper.selectList(
                        new QueryWrapper<TestimonialEntity>()
                                .eq("is_enabled", 1)
                                .orderByAsc("sort")
                                .orderByAsc("id")
                )
                .stream()
                .map(this::toVo)
                .toList();
    }

    private TestimonialVO toVo(TestimonialEntity e) {
        TestimonialVO v = new TestimonialVO();
        v.setId(e.getId());
        v.setAvatarUrl(normalizeAvatarUrl(e.getAvatarUrl()));
        v.setName(e.getName());
        v.setTitle(e.getTitle());
        v.setStarRating(e.getStarRating());
        v.setReviewText(e.getReviewText());
        return v;
    }

    /**
     * 兼容旧版头像 URL。
     *
     * <p>早期 storeTestimonialAvatar 返回 /uploads/testimonial/avatar/...，线上 Nginx 只代理了 /api/v1/admin，
     * 导致旧头像裂图。读数据时自动把旧路径改写为 /api/v1/admin/uploads/...，新路径不受影响。
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && avatarUrl.startsWith("/uploads/")) {
            return "/api/v1/admin" + avatarUrl;
        }
        return avatarUrl;
    }
}
