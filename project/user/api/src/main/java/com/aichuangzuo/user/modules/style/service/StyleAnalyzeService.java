package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;

/**
 * 风格分析服务：调大模型拆解参考文章的写作风格。
 */
public interface StyleAnalyzeService {

    /**
     * 分析参考文章风格。
     *
     * @param text 参考文章正文（200-3000 字，Controller 层已校验）
     * @return 风格提示词 + 2 段原文摘录
     */
    StyleAnalyzeVO analyze(String text);
}
