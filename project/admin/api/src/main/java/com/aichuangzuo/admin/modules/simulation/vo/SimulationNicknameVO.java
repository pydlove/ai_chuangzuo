package com.aichuangzuo.admin.modules.simulation.vo;

import java.time.LocalDateTime;

/**
 * 模拟运营-昵称库列表项。
 */
public record SimulationNicknameVO(Long id, String nickname, LocalDateTime createdAt) {
}
