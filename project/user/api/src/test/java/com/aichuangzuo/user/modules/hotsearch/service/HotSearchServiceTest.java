package com.aichuangzuo.user.modules.hotsearch.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.enums.HotSearchErrorCode;
import com.aichuangzuo.user.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class HotSearchServiceTest {

    @Autowired
    private HotSearchService hotSearchService;

    @Autowired
    private HotSearchPlatformMapper platformMapper;

    @Autowired
    private HotSearchDailyMapper dailyMapper;

    @Autowired
    private HotSearchCrawlJob crawlJob;

    @Test
    void shouldListEnabledPlatforms() {
        List<HotSearchPlatformVO> list = hotSearchService.listPlatforms();
        assertTrue(list.size() >= 5);
        assertTrue(list.stream().anyMatch(p -> "douyin".equals(p.getCode())));
    }

    @Test
    void shouldReturnDailyList() {
        HotSearchDaily daily = new HotSearchDaily();
        daily.setPlatformCode("douyin");
        daily.setRankNum(1);
        daily.setTitle("测试热搜");
        daily.setHotValue("100万");
        daily.setSnapshotDate(LocalDate.now());
        dailyMapper.insert(daily);

        List<HotSearchItemVO> list = hotSearchService.listByPlatformAndDate("douyin", LocalDate.now());
        assertEquals(1, list.size());
        assertEquals("测试热搜", list.get(0).getTitle());
    }

    @Test
    void shouldRejectDisabledPlatform() {
        HotSearchPlatform platform = platformMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HotSearchPlatform>()
                        .eq(HotSearchPlatform::getCode, "baidu"));
        platform.setEnabled(0);
        platformMapper.updateById(platform);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> hotSearchService.listByPlatformAndDate("baidu", LocalDate.now()));
        assertEquals(HotSearchErrorCode.PLATFORM_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void shouldTriggerManualCrawl() {
        hotSearchService.crawl();
        // 由于真实平台可能抓取失败，只要执行不抛异常即可
        assertTrue(true);
    }
}
