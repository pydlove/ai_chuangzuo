package com.aichuangzuo.user.modules.hotsearch.job;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.user.modules.hotsearch.mapper.HotSearchPlatformMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Rollback
class HotSearchCrawlJobTest {

    @Autowired
    private HotSearchCrawlJob crawlJob;

    @Autowired
    private HotSearchPlatformMapper platformMapper;

    @Autowired
    private HotSearchDailyMapper dailyMapper;

    @Test
    void shouldCrawlMockDataIntoDb() {
        HotSearchPlatform mockPlatform = new HotSearchPlatform();
        mockPlatform.setCode("mock");
        mockPlatform.setName("Mock");
        mockPlatform.setSortOrder(99);
        mockPlatform.setEnabled(1);
        platformMapper.insert(mockPlatform);

        crawlJob.crawl();

        LocalDate today = LocalDate.now();
        assertEquals(5, dailyMapper.selectByPlatformAndDate("mock", today).size());
    }
}
