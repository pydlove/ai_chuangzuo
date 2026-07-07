package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 jsoup 的公开页面 / JSON 抓取器：仅处理无需 JS 引擎的平台（百度、抖音）。
 * 微博 / B 站 / 头条 由 CdpHotSearchFetcher 处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupHotSearchFetcher implements HotSearchFetcher {

    private final HotSearchProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(HotSearchPlatform platform) {
        String code = platform.getCode();
        return "baidu".equals(code) || "douyin".equals(code);
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        return switch (platform.getCode()) {
            case "baidu" -> fetchBaidu();
            case "douyin" -> fetchDouyin();
            default -> new ArrayList<>();
        };
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(properties.getConnectTimeoutMillis())
                .get();
    }

    private List<HotSearchItem> fetchBaidu() {
        try {
            Document doc = fetchDocument("https://top.baidu.com/board?tab=realtime");
            Elements rows = doc.select(".category-wrap_iQLoo");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element titleEl = row.selectFirst(".c-single-text-ellipsis");
                Element hotEl = row.selectFirst(".hot-index_1Bl1a");
                if (titleEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(titleEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("百度热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchDouyin() {
        // 抖音热搜走 aweme/v1/web/hot/search/list JSON 接口，无登录可用
        try {
            String url = "https://www.douyin.com/aweme/v1/web/hot/search/list/"
                    + "?device_platform=webapp&aid=6383&channel=channel_pc_web"
                    + "&version_code=190500&version_name=19.5.0"
                    + "&cookie_enabled=true&platform=PC";
            String body = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Referer", "https://www.douyin.com/")
                    .ignoreContentType(true)
                    .timeout(properties.getReadTimeoutMillis())
                    .get()
                    .body()
                    .text();
            JsonNode root = objectMapper.readTree(body);
            JsonNode wordList = root.path("data").path("word_list");
            if (!wordList.isArray()) {
                log.warn("抖音热搜响应格式异常：无 word_list");
                return new ArrayList<>();
            }
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (JsonNode w : wordList) {
                if (rank > properties.getTopN()) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(w.path("word").asText(""));
                JsonNode hv = w.path("hot_value");
                item.setHotValue(hv.isNumber() ? String.valueOf(hv.asLong()) : hv.asText(null));
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("抖音热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
