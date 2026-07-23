package com.aichuangzuo.admin.modules.style.review.service;

import com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest;
import com.aichuangzuo.admin.modules.style.review.vo.StyleReviewVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 风格审核服务。
 */
public interface StyleReviewService {

    /**
     * 分页查询风格审核列表。
     */
    IPage<StyleReviewVO> page(StyleReviewPageRequest request);

    /**
     * 通过风格审核（pending → approved）。
     */
    void approve(String bizNo);

    /**
     * 批量通过风格审核。
     *
     * @param bizNos 业务编号列表
     * @return 实际通过数量
     */
    int batchApprove(List<String> bizNos);

    /**
     * 打回风格审核（pending/approved → rejected）。
     */
    void reject(String bizNo, String reason);
}