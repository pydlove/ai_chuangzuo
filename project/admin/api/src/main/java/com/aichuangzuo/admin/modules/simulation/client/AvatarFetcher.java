package com.aichuangzuo.admin.modules.simulation.client;

import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * 模拟运营-头像抓取器：从 picsum.photos 拉取随机图片字节。
 */
@Slf4j
@Component
public class AvatarFetcher {

    private final RestTemplate restTemplate = new RestTemplate();

    /** 拉取一张随机 200x200 图片，返回字节；失败抛 BusinessException。 */
    public byte[] fetchRandomAvatar() {
        String seed = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String url = "https://picsum.photos/seed/" + seed + "/200/200";
        try {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new BusinessException(500, "模拟运营头像下载为空");
            }
            log.info("模拟运营头像下载成功 url={} size={}", url, bytes.length);
            return bytes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("模拟运营头像下载失败 url={}", url, e);
            throw new BusinessException(500, "模拟运营头像下载失败");
        }
    }
}
