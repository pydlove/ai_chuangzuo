package com.aichuangzuo.admin.modules.skill.review.service;

import com.aichuangzuo.admin.modules.skill.review.dto.request.SkillReviewPageRequest;
import com.aichuangzuo.admin.modules.skill.review.vo.SkillReviewVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 风格审核服务。
 */
public interface SkillReviewService {

    /**
     * 分页查询风格审核列表。
     */
    IPage<SkillReviewVO> page(SkillReviewPageRequest request);

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

    /**
     * 删除审核列表中的脏数据：将无待审核市场记录的待审核残留记录回写为草稿（audit_status=3），
     * 使其移出审核列表，同时保留用户在自己的提示词列表中的数据。
     *
     * <p>真实待审核（存在待审核市场记录）的提交不允许删除，必须通过或打回。
     */
    void deleteDirty(String bizNo);
}