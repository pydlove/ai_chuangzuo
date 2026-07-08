package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.user.modules.style.vo.SystemStyleVO;

import java.util.List;

/**
 * 系统预设风格（{@code source_type=3}）服务。
 */
public interface SystemStyleService {

    /**
     * 列出当前启用的系统预设风格，可按名称关键词过滤。
     */
    List<SystemStyleVO> listEnabled(String keyword);
}