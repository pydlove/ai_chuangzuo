package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchPlatformAdminService;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotSearchPlatformAdminServiceImpl implements HotSearchPlatformAdminService {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;

    @Override
    public List<HotSearchPlatform> listAll() {
        return platformMapper.selectList(
                new LambdaQueryWrapper<HotSearchPlatform>().orderByAsc(HotSearchPlatform::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchPlatform create(HotSearchPlatformRequest req) {
        Long exist = platformMapper.selectCount(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, req.getCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_CODE_DUPLICATED);
        }
        HotSearchPlatform p = new HotSearchPlatform();
        p.setCode(req.getCode());
        p.setName(req.getName());
        p.setIcon(req.getIcon());
        p.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        p.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        platformMapper.insert(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchPlatform update(Long id, HotSearchPlatformRequest req) {
        HotSearchPlatform p = platformMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        p.setName(req.getName());
        p.setIcon(req.getIcon());
        p.setSortOrder(req.getSortOrder() == null ? p.getSortOrder() : req.getSortOrder());
        p.setEnabled(req.getEnabled() == null ? p.getEnabled() : req.getEnabled());
        p.setUpdatedAt(LocalDateTime.now());
        platformMapper.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HotSearchPlatform p = platformMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        Long refCount = dailyMapper.selectCount(
                new LambdaQueryWrapper<HotSearchDaily>()
                        .eq(HotSearchDaily::getPlatformCode, p.getCode()));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_IN_USE);
        }
        platformMapper.deleteById(id);
    }
}
