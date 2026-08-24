package com.aichuangzuo.user.modules.workbench.service.impl;

import com.aichuangzuo.user.modules.workbench.dto.request.SaveWeeklyArticlesRequest;
import com.aichuangzuo.user.modules.workbench.dto.request.WeeklyArticleItemRequest;
import com.aichuangzuo.user.modules.workbench.entity.WeeklyArticle;
import com.aichuangzuo.user.modules.workbench.mapper.WeeklyArticleMapper;
import com.aichuangzuo.user.modules.workbench.service.WeeklyArticleService;
import com.aichuangzuo.user.modules.workbench.vo.WeeklyArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;

/**
 * 工作台每周文章数据服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyArticleServiceImpl implements WeeklyArticleService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final WeeklyArticleMapper weeklyArticleMapper;

    @Override
    public List<WeeklyArticleVO> getCurrentWeekArticles(Long userId) {
        LocalDate weekStart = currentWeekStart();
        log.info("查询本周文章数据, userId={}, weekStart={}", userId, weekStart);
        return weeklyArticleMapper.selectByUserAndWeek(userId, weekStart).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCurrentWeekArticles(Long userId, SaveWeeklyArticlesRequest request) {
        LocalDate weekStart = currentWeekStart();
        log.info("保存本周文章数据, userId={}, weekStart={}, count={}", userId, weekStart, request.getArticles().size());

        weeklyArticleMapper.deleteByUserAndWeek(userId, weekStart);

        for (WeeklyArticleItemRequest item : request.getArticles()) {
            WeeklyArticle entity = new WeeklyArticle();
            entity.setUserId(userId);
            entity.setWeekStartDate(weekStart);
            entity.setTitle(item.getTitle().trim());
            entity.setReads(item.getReads());
            weeklyArticleMapper.insert(entity);
        }
    }

    private WeeklyArticleVO toVO(WeeklyArticle entity) {
        WeeklyArticleVO vo = new WeeklyArticleVO();
        vo.setTitle(entity.getTitle());
        vo.setReads(entity.getReads());
        return vo;
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now(ZONE).with(WeekFields.ISO.dayOfWeek(), 1L);
    }
}
