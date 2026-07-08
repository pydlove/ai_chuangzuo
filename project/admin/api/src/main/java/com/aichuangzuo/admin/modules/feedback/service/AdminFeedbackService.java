package com.aichuangzuo.admin.modules.feedback.service;

import com.aichuangzuo.admin.modules.feedback.vo.AdminFeedbackVO;

import java.util.List;

public interface AdminFeedbackService {
    List<AdminFeedbackVO> page(Integer status, long pageNum, long size);
    long count(Integer status);
    AdminFeedbackVO detail(Long id);
    void reply(Long id, Long adminId, String content);
}
