package com.aichuangzuo.admin.modules.security.accesscontrol.service;

import com.aichuangzuo.admin.modules.security.accesscontrol.dto.request.AccessControlCreateRequest;
import com.aichuangzuo.admin.modules.security.accesscontrol.dto.request.AccessControlUpdateRequest;
import com.aichuangzuo.admin.modules.security.accesscontrol.entity.AccessControl;
import com.aichuangzuo.admin.modules.security.accesscontrol.mapper.AccessControlMapper;
import com.aichuangzuo.admin.modules.security.accesscontrol.vo.AccessControlVO;
import com.aichuangzuo.shared.enums.error.AdminSecurityErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AccessControlMapper accessControlMapper;

    public IPage<AccessControlVO> page(Integer ruleType, Integer listType, String keyword, int page, int size) {
        LambdaQueryWrapper<AccessControl> wrapper = Wrappers.<AccessControl>lambdaQuery()
                .eq(AccessControl::getIsDeleted, 0)
                .eq(ruleType != null, AccessControl::getRuleType, ruleType)
                .eq(listType != null, AccessControl::getListType, listType)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(AccessControl::getRuleValue, keyword)
                        .or()
                        .like(AccessControl::getRemark, keyword))
                .orderByDesc(AccessControl::getCreatedAt);

        Page<AccessControl> entityPage = new Page<>(page, size);
        IPage<AccessControl> result = accessControlMapper.selectPage(entityPage, wrapper);

        List<AccessControlVO> records = result.getRecords().stream()
                .map(this::toVo)
                .toList();

        Page<AccessControlVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Transactional(rollbackFor = Exception.class)
    public AccessControlVO create(AccessControlCreateRequest request, Long adminUserId) {
        String value = StringUtils.trimWhitespace(request.getRuleValue());
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_VALUE_EMPTY);
        }

        Long exists = accessControlMapper.selectCount(Wrappers.<AccessControl>lambdaQuery()
                .eq(AccessControl::getIsDeleted, 0)
                .eq(AccessControl::getRuleType, request.getRuleType())
                .eq(AccessControl::getListType, request.getListType())
                .eq(AccessControl::getRuleValue, value));
        if (exists != null && exists > 0) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_EXISTS);
        }

        AccessControl entity = new AccessControl();
        entity.setRuleType(request.getRuleType());
        entity.setListType(request.getListType());
        entity.setRuleValue(value);
        entity.setRemark(StringUtils.trimWhitespace(request.getRemark()));
        entity.setRuleStatus(request.getRuleStatus());
        entity.setIsDeleted(0);
        entity.setCreatedBy(adminUserId == null ? 0L : adminUserId);
        entity.setUpdatedBy(adminUserId == null ? 0L : adminUserId);

        accessControlMapper.insert(entity);
        log.info("管理员创建访问控制规则 adminUserId={} ruleType={} listType={} value={}",
                adminUserId, request.getRuleType(), request.getListType(), value);
        return toVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public AccessControlVO update(Long id, AccessControlUpdateRequest request, Long adminUserId) {
        AccessControl entity = accessControlMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getIsDeleted())) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_NOT_FOUND);
        }

        String value = StringUtils.trimWhitespace(request.getRuleValue());
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_VALUE_EMPTY);
        }

        Long exists = accessControlMapper.selectCount(Wrappers.<AccessControl>lambdaQuery()
                .eq(AccessControl::getIsDeleted, 0)
                .eq(AccessControl::getRuleType, entity.getRuleType())
                .eq(AccessControl::getListType, entity.getListType())
                .eq(AccessControl::getRuleValue, value)
                .ne(AccessControl::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_EXISTS);
        }

        entity.setRuleValue(value);
        entity.setRemark(StringUtils.trimWhitespace(request.getRemark()));
        entity.setRuleStatus(request.getRuleStatus());
        entity.setUpdatedBy(adminUserId == null ? 0L : adminUserId);

        accessControlMapper.updateById(entity);
        log.info("管理员更新访问控制规则 adminUserId={} id={} value={}", adminUserId, id, value);
        return toVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long adminUserId) {
        AccessControl entity = accessControlMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getIsDeleted())) {
            throw new BusinessException(AdminSecurityErrorCode.ACCESS_CONTROL_RULE_NOT_FOUND);
        }

        entity.setIsDeleted(1);
        entity.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        accessControlMapper.updateById(entity);
        log.info("管理员删除访问控制规则 adminUserId={} id={}", adminUserId, id);
    }

    private AccessControlVO toVo(AccessControl entity) {
        AccessControlVO vo = new AccessControlVO();
        vo.setId(entity.getId());
        vo.setRuleType(entity.getRuleType());
        vo.setListType(entity.getListType());
        vo.setRuleValue(entity.getRuleValue());
        vo.setRuleStatus(entity.getRuleStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
