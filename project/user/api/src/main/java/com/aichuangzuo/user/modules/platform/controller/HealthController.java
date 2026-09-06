package com.aichuangzuo.user.modules.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务健康检查接口（免登录）。
 *
 * <p>前端版本心跳在刷新前探测本接口，确认后端真正就绪后再 reload，
 * 避免新版本静态文件已部署、但后端仍在重启时刷新报错。</p>
 */
@RestController
@RequestMapping("/api/v1/public")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "up"));
    }
}
