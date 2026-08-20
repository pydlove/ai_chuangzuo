package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.NicknameCheckRequest;
import com.aichuangzuo.user.modules.selfmedia.service.NicknameCheckAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.NicknameCheckVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@Slf4j
@Tag(name = "用户端-平台账号检测")
@RestController
@RequestMapping("/api/v1/user/self-media/nickname")
@RequiredArgsConstructor
public class PlatformAccountCheckController {

    private final NicknameCheckAiService nicknameCheckAiService;
    private final SelfMediaPlanService selfMediaPlanService;

    @PostMapping("/check")
    public Result<NicknameCheckVO> check(@Valid @RequestBody NicknameCheckRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        SelfMediaPlanVO plan = selfMediaPlanService.getCurrentPlan(userId);
        String platform = StringUtils.defaultIfBlank(request.getPlatform(),
                plan == null ? "" : StringUtils.defaultString(plan.getPlatformName(), plan.getPlatformKey()));
        String positioning = StringUtils.defaultIfBlank(request.getPositioning(), buildPositioning(plan));
        log.info("平台账号名称检测, userId={}, nickname={}, platform={}, hasPlan={}",
                userId, request.getNickname(), platform, plan != null);
        NicknameCheckVO vo = nicknameCheckAiService.checkNickname(platform, positioning, request.getNickname());
        log.info("平台账号名称检测完成, userId={}, nickname={}, fit={}, reason={}",
                userId, request.getNickname(), vo.getFit(), StringUtils.abbreviate(vo.getReason(), 100));
        return Result.success(vo);
    }

    private String buildPositioning(SelfMediaPlanVO plan) {
        if (plan == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("平台：").append(StringUtils.defaultString(plan.getPlatformName(), plan.getPlatformKey())).append("；");
        sb.append("赛道：").append(StringUtils.defaultString(plan.getNicheName(), plan.getNicheKey())).append("；");
        sb.append("人设：").append(StringUtils.defaultString(plan.getPersonaName(), plan.getPersonaKey())).append("；");
        if (plan.getPillars() != null && !plan.getPillars().isEmpty()) {
            String pillars = plan.getPillars().stream()
                    .map(p -> p.getName() + " " + p.getPercent() + "%")
                    .collect(Collectors.joining("，"));
            sb.append("内容支柱：").append(pillars).append("。");
        }
        return sb.toString();
    }
}

