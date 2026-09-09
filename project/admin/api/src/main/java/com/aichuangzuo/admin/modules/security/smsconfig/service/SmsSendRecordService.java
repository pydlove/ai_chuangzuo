
package com.aichuangzuo.admin.modules.security.smsconfig.service;

import com.aichuangzuo.admin.modules.security.smsconfig.dto.request.SmsSendRecordQueryRequest;
import com.aichuangzuo.admin.modules.security.smsconfig.entity.SmsSendRecord;
import com.aichuangzuo.admin.modules.security.smsconfig.mapper.SmsSendRecordMapper;
import com.aichuangzuo.admin.modules.security.smsconfig.vo.SmsSendRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 短信发送记录查询服务（用户端写入，管理端只读）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSendRecordService {

    private final SmsSendRecordMapper mapper;

    public PageResult<SmsSendRecordVO> list(SmsSendRecordQueryRequest request) {
        Page<SmsSendRecord> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<SmsSendRecord> wrapper = new LambdaQueryWrapper<SmsSendRecord>()
                .orderByDesc(SmsSendRecord::getCreatedAt);
        if (StringUtils.hasText(request.getPhone())) {
            wrapper.like(SmsSendRecord::getPhone, request.getPhone().trim());
        }
        if (StringUtils.hasText(request.getScene())) {
            wrapper.eq(SmsSendRecord::getScene, request.getScene().trim());
        }
        if (request.getSendStatus() != null) {
            wrapper.eq(SmsSendRecord::getSendStatus, request.getSendStatus());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(SmsSendRecord::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(SmsSendRecord::getCreatedAt, request.getEndTime());
        }
        Page<SmsSendRecord> result = mapper.selectPage(page, wrapper);
        List<SmsSendRecordVO> items = result.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private SmsSendRecordVO toVo(SmsSendRecord record) {
        SmsSendRecordVO vo = new SmsSendRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    public record PageResult<T>(List<T> items, long total, long page, long size) {
    }
}
