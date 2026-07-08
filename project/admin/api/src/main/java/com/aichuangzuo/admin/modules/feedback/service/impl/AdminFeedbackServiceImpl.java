package com.aichuangzuo.admin.modules.feedback.service.impl;

import com.aichuangzuo.admin.modules.feedback.entity.AdminFeedbackView;
import com.aichuangzuo.admin.modules.feedback.enums.AdminFeedbackErrorCode;
import com.aichuangzuo.admin.modules.feedback.mapper.AdminFeedbackMapper;
import com.aichuangzuo.admin.modules.feedback.service.AdminFeedbackService;
import com.aichuangzuo.admin.modules.feedback.vo.AdminFeedbackVO;
import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.mapper.MessageAggregateMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private final AdminFeedbackMapper feedbackMapper;
    private final MessageAggregateMapper messageMapper;

    @Override
    public List<AdminFeedbackVO> page(Integer status, long pageNum, long size) {
        long offset = (pageNum - 1) * size;
        return feedbackMapper.selectPage(status, offset, size).stream()
                .map(this::toVO).toList();
    }

    @Override
    public long count(Integer status) {
        return feedbackMapper.countPage(status);
    }

    @Override
    public AdminFeedbackVO detail(Long id) {
        AdminFeedbackView v = feedbackMapper.selectById(id);
        if (v == null) throw new BusinessException(AdminFeedbackErrorCode.FEEDBACK_NOT_FOUND);
        return toVO(v);
    }

    @Override
    @Transactional
    public void reply(Long id, Long adminId, String content) {
        AdminFeedbackView v = feedbackMapper.selectById(id);
        if (v == null) throw new BusinessException(AdminFeedbackErrorCode.FEEDBACK_NOT_FOUND);
        if (v.getStatus() != null && v.getStatus() == 1) {
            throw new BusinessException(AdminFeedbackErrorCode.ALREADY_REPLIED);
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = feedbackMapper.markReplied(id, content, adminId, now, adminId);
        if (affected == 0) {
            throw new BusinessException(AdminFeedbackErrorCode.ALREADY_REPLIED);
        }
        try {
            MessageAggregate m = new MessageAggregate();
            m.setBizNo("FBK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            m.setMsgType("feedback");
            m.setScope(2);
            m.setTargetUserId(v.getUserId());
            m.setTitle("您的反馈已收到回复");
            m.setSummary("点击查看管理员的回复");
            m.setContent(content);
            m.setSubType("reply");
            m.setTenantId(0L);
            m.setCreatedBy(adminId);
            m.setUpdatedBy(adminId);
            messageMapper.insertReply(m);
        } catch (Exception ex) {
            log.warn("发送反馈回复消息失败 feedbackId={} reason={}", id, ex.getMessage());
        }
    }

    private AdminFeedbackVO toVO(AdminFeedbackView v) {
        AdminFeedbackVO vo = new AdminFeedbackVO();
        BeanUtils.copyProperties(v, vo);
        return vo;
    }
}
