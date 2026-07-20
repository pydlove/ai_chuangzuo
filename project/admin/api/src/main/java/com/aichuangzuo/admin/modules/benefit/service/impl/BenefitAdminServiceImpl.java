package com.aichuangzuo.admin.modules.benefit.service.impl;

import com.aichuangzuo.admin.modules.benefit.entity.Benefit;
import com.aichuangzuo.admin.modules.benefit.mapper.BenefitMapper;
import com.aichuangzuo.admin.modules.benefit.service.BenefitAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BenefitAdminServiceImpl implements BenefitAdminService {

    private final BenefitMapper benefitMapper;

    @Override
    public List<Benefit> list() {
        return benefitMapper.selectList(new LambdaQueryWrapper<Benefit>()
                .orderByAsc(Benefit::getSortOrder));
    }
}