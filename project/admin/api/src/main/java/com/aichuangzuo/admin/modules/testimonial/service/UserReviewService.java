package com.aichuangzuo.admin.modules.testimonial.service;

import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewPageRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.UserReviewStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.vo.UserReviewVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface UserReviewService {

    IPage<UserReviewVO> page(UserReviewPageRequest request);

    void updateStatus(Long id, UserReviewStatusRequest request);
}
