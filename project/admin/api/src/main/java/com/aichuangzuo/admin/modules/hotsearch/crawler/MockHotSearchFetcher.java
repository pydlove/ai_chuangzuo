package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 抓取器，返回固定演示数据；用于本地开发和集成测试。
 */
@Component
public class MockHotSearchFetcher implements HotSearchFetcher {

    @Override
    public boolean supports(HotSearchPlatform platform) {
        return "mock".equals(platform.getCode());
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        List<HotSearchItem> list = new ArrayList<>();
        String[] titles = {
                "这是属于我们的夏天",
                "普通人如何靠副业月入过万",
                "原来这些方法真的能让人变自律",
                "90 后夫妻裸辞返乡创业日记",
                "被这条视频治愈了一整天"
        };
        for (int i = 0; i < titles.length; i++) {
            HotSearchItem item = new HotSearchItem();
            item.setRank(i + 1);
            item.setTitle(titles[i]);
            item.setHotValue((500 - i * 20) + "万");
            item.setUrl(null);
            list.add(item);
        }
        return list;
    }
}
