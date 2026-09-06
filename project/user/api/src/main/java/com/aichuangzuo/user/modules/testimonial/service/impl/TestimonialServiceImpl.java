package com.aichuangzuo.user.modules.testimonial.service.impl;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.aichuangzuo.user.modules.feedback.mapper.FeedbackMapper;
import com.aichuangzuo.user.modules.testimonial.entity.TestimonialEntity;
import com.aichuangzuo.user.modules.testimonial.mapper.TestimonialMapper;
import com.aichuangzuo.user.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.user.modules.testimonial.vo.TestimonialVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialMapper mapper;
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    @Override
    public List<TestimonialVO> listEnabled(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int fetchLimit = safePage * safeSize;

        List<TestimonialVO> combined = new ArrayList<>(fetchLimit);

        // 模拟评价：管理员手动创建/导入的，按 sort 升序
        List<TestimonialEntity> simulated = mapper.selectList(
                new QueryWrapper<TestimonialEntity>()
                        .eq("is_enabled", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id")
                        .last("LIMIT " + fetchLimit)
        );
        for (TestimonialEntity e : simulated) {
            combined.add(toVo(e));
        }

        // 用户评价：从反馈表同步、且管理员同意展示到首页的，按时间倒序
        List<Feedback> userReviews = feedbackMapper.findReviewsShownOnHomepage(fetchLimit);
        for (Feedback fb : userReviews) {
            combined.add(feedbackToVo(fb));
        }

        int offset = (safePage - 1) * safeSize;
        if (offset >= combined.size()) {
            return List.of();
        }
        return combined.subList(offset, Math.min(offset + safeSize, combined.size()));
    }

    private TestimonialVO toVo(TestimonialEntity e) {
        TestimonialVO v = new TestimonialVO();
        v.setId(e.getId());
        v.setAvatarUrl(normalizeAvatarUrl(e.getAvatarUrl()));
        v.setName(e.getName());
        v.setTitle(e.getTitle());
        v.setStarRating(e.getStarRating());
        v.setReviewText(e.getReviewText());
        v.setSource("simulated");
        return v;
    }

    private TestimonialVO feedbackToVo(Feedback fb) {
        TestimonialVO v = new TestimonialVO();
        v.setId(fb.getId());

        User user = userMapper.selectById(fb.getUserId());
        if (user != null) {
            v.setAvatarUrl(user.getAvatarUrl());
            v.setName(user.getNickname());
        }
        v.setTitle("");
        v.setStarRating(fb.getStarRating());
        v.setReviewText(fb.getContent());
        v.setSource("user");
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
