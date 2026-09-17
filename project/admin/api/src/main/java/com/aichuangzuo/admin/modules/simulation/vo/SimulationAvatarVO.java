package com.aichuangzuo.admin.modules.simulation.vo;

import java.time.LocalDateTime;

/**
 * 模拟运营-头像库列表项（dataUrl 直接内联展示，前端无需再请求图片接口）。
 */
public record SimulationAvatarVO(Long id, String dataUrl, LocalDateTime createdAt) {
}
