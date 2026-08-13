package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.generation.entity.GenerationTaskRefund;
import com.aichuangzuo.user.modules.generation.mapper.GenerationTaskRefundMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 生成任务额度退款服务，保证同一任务同一权益只退一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskRefundService {

    private final GenerationTaskRefundMapper refundMapper;
    private final BenefitService benefitService;

    @Transactional(rollbackFor = Exception.class)
    public void refundOnce(Long taskId, Long userId, String benefitCode) {
        if (taskId == null || userId == null || benefitCode == null || benefitCode.isBlank()) {
            throw new IllegalArgumentException("taskId、userId、benefitCode 不能为空");
        }

        LambdaQueryWrapper<GenerationTaskRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenerationTaskRefund::getTaskId, taskId)
               .eq(GenerationTaskRefund::getBenefitCode, benefitCode);
        if (refundMapper.selectCount(wrapper) > 0) {
            log.info("任务额度已退过，跳过 refund taskId={}, benefitCode={}", taskId, benefitCode);
            return;
        }

        GenerationTaskRefund record = new GenerationTaskRefund();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setBenefitCode(benefitCode);
        record.setRefundedAt(LocalDateTime.now());

        try {
            refundMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.info("并发退款记录已存在，跳过 refund taskId={}, benefitCode={}", taskId, benefitCode);
            return;
        }

        benefitService.refund(userId, benefitCode);
        log.info("任务额度退款成功 taskId={}, userId={}, benefitCode={}", taskId, userId, benefitCode);
    }
}
