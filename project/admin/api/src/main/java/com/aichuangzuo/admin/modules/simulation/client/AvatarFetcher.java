package com.aichuangzuo.admin.modules.simulation.client;

import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 模拟运营-头像抓取器：从 randomuser.me 拉取真人头像，按编号去重。
 *
 * <p>编号 1-100 = men/0-99，101-200 = women/0-99；单个编号下载失败时顺延下一个未用编号。
 */
@Slf4j
@Component
public class AvatarFetcher {

    /** randomuser.me 头像库：男女各 100 张，共 200 个唯一编号。 */
    static final int IMG_COUNT = 200;

    /** 单次抓取最多尝试的候选数。 */
    private static final int MAX_TRIES = 5;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 抓取结果：图片字节 + 编号（编号落库保证跨批次不重复）。 */
    public record Avatar(byte[] bytes, int img) {
    }

    /**
     * 从未被使用的编号中随机取一张 200x200 真人头像；候选下载失败时顺延下一个。
     */
    public Avatar fetchRandomAvatar(Set<Integer> usedImgs) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= IMG_COUNT; i++) {
            if (usedImgs == null || !usedImgs.contains(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            throw new BusinessException(500, "模拟运营头像库已用尽（" + IMG_COUNT + "张）");
        }
        Collections.shuffle(candidates);
        BusinessException lastError = null;
        for (int i = 0; i < Math.min(MAX_TRIES, candidates.size()); i++) {
            int img = candidates.get(i);
            try {
                byte[] bytes = download(img);
                log.info("模拟运营头像下载成功 img={} size={}", img, bytes.length);
                return new Avatar(bytes, img);
            } catch (BusinessException e) {
                lastError = e;
                log.warn("模拟运营头像下载失败 img={}，顺延下一个候选", img);
            }
        }
        throw lastError == null
                ? new BusinessException(500, "模拟运营头像下载失败")
                : lastError;
    }

    private byte[] download(int img) {
        String url = "https://randomuser.me/api/portraits/" + (img <= 100 ? "men/" + (img - 1) : "women/" + (img - 101)) + ".jpg";
        try {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new BusinessException(500, "模拟运营头像下载为空");
            }
            return bytes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "模拟运营头像下载失败");
        }
    }
}
