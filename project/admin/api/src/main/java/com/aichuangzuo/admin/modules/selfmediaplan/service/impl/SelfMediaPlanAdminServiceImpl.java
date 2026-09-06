package com.aichuangzuo.admin.modules.selfmediaplan.service.impl;

import com.aichuangzuo.admin.modules.selfmediaplan.dto.request.SelfMediaPlanPageRequest;
import com.aichuangzuo.admin.modules.selfmediaplan.entity.UserSelfMediaPlan;
import com.aichuangzuo.admin.modules.selfmediaplan.mapper.UserSelfMediaPlanMapper;
import com.aichuangzuo.admin.modules.selfmediaplan.service.SelfMediaPlanAdminService;
import com.aichuangzuo.admin.modules.selfmediaplan.vo.SelfMediaPlanVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelfMediaPlanAdminServiceImpl implements SelfMediaPlanAdminService {

    private final UserSelfMediaPlanMapper planMapper;
    private final PlatformUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<SelfMediaPlanVO> page(SelfMediaPlanPageRequest request) {
        QueryWrapper<UserSelfMediaPlan> wrapper = new QueryWrapper<UserSelfMediaPlan>()
                .orderByDesc("id");

        String keyword = request.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            List<Long> matchedUserIds = matchUserIds(keyword.trim());
            if (matchedUserIds.isEmpty()) {
                return new Page<>(request.getPage(), request.getPageSize(), 0);
            }
            wrapper.in("user_id", matchedUserIds);
        }

        Page<UserSelfMediaPlan> result = planMapper.selectPage(new Page<>(request.getPage(), request.getPageSize()), wrapper);
        Map<Long, PlatformUser> userMap = loadUsers(result.getRecords());

        List<SelfMediaPlanVO> records = result.getRecords().stream()
                .map(plan -> toVo(plan, userMap.get(plan.getUserId())))
                .toList();
        Page<SelfMediaPlanVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private List<Long> matchUserIds(String keyword) {
        QueryWrapper<PlatformUser> userWrapper = new QueryWrapper<PlatformUser>()
                .select("id")
                .and(w -> w.like("nickname", keyword)
                        .or().like("phone", keyword)
                        .or().like("email", keyword));
        return userMapper.selectList(userWrapper).stream()
                .map(PlatformUser::getId)
                .toList();
    }

    private Map<Long, PlatformUser> loadUsers(List<UserSelfMediaPlan> plans) {
        List<Long> userIds = plans.stream()
                .map(UserSelfMediaPlan::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(PlatformUser::getId, Function.identity()));
    }

    private SelfMediaPlanVO toVo(UserSelfMediaPlan plan, PlatformUser user) {
        SelfMediaPlanVO vo = new SelfMediaPlanVO();
        vo.setId(plan.getId());
        vo.setUserId(plan.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
            vo.setUserEmail(user.getEmail());
        }
        vo.setPlatformKey(plan.getPlatformKey());
        vo.setPlatformName(plan.getPlatformName());
        vo.setNicheName(plan.getNicheName());
        vo.setPersonaName(plan.getPersonaName());
        vo.setContentPillars(parsePillars(plan.getContentPillarsJson()));
        vo.setIsRecommendedByAi(plan.getIsRecommendedByAi());
        vo.setCreatedAt(plan.getCreatedAt());
        vo.setUpdatedAt(plan.getUpdatedAt());
        return vo;
    }

    private List<SelfMediaPlanVO.Pillar> parsePillars(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SelfMediaPlanVO.Pillar>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
