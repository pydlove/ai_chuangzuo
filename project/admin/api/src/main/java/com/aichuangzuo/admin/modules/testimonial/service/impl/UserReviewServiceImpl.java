package com.aichuangzuo.admin.modules.testimonial.service.impl;

import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewPageRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.entity.UserReviewEntity;
import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.admin.modules.testimonial.mapper.UserReviewMapper;
import com.aichuangzuo.admin.modules.testimonial.service.UserReviewService;
import com.aichuangzuo.admin.modules.testimonial.vo.UserReviewVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserReviewServiceImpl implements UserReviewService {

    private final UserReviewMapper userReviewMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public IPage<UserReviewVO> page(UserReviewPageRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 20 : request.getPageSize();
        pageSize = Math.min(pageSize, 100);

        long total = userReviewMapper.countReviews(blankToNull(request.getKeyword()));
        int offset = (pageNum - 1) * pageSize;
        List<UserReviewEntity> rows = userReviewMapper.pageReviews(blankToNull(request.getKeyword()), offset, pageSize);

        List<UserReviewVO> vos = rows.stream()
                .map(this::toVo)
                .toList();

        Page<UserReviewVO> voPage = new Page<>(pageNum, pageSize, total);
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public void updateStatus(Long id, UserReviewStatusRequest request) {
        Integer status = request.getIsShowOnHomepage();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(TestimonialErrorCode.TESTIMONIAL_STATUS_INVALID);
        }
        int affected = userReviewMapper.updateShowOnHomepage(id, status);
        if (affected == 0) {
            throw new BusinessException(TestimonialErrorCode.TESTIMONIAL_NOT_FOUND);
        }
    }

    private UserReviewVO toVo(UserReviewEntity e) {
        UserReviewVO v = new UserReviewVO();
        v.setId(e.getId());
        v.setUserId(e.getUserId());
        v.setContent(e.getContent());
        v.setStarRating(e.getStarRating());
        v.setStatus(e.getStatus());
        v.setIsShowOnHomepage(e.getIsShowOnHomepage());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());

        if (e.getUserId() != null) {
            PlatformUser user = platformUserMapper.selectById(e.getUserId());
            if (user != null) {
                v.setNickname(user.getNickname());
            }
        }
        return v;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
