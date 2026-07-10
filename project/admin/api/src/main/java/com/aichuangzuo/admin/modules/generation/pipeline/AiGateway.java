package com.aichuangzuo.admin.modules.generation.pipeline;

/**
 * AI 统一调用入口。
 *
 * <p>所有 step 调 AI 都走这里，自动做：
 * <ul>
 *   <li>budget 校验：ctx.aiCallUsed >= ctx.aiCallBudget 时抛 {@code AiBudgetExhaustedException}</li>
 *   <li>调用计数：ctx.aiCallUsed++ + ctx.aiCallTotalMs 累加</li>
 *   <li>历史留痕：ctx.aiCallHistory append</li>
 *   <li>失败语义：异常被捕获，aiCallFailed++，记录 error，<b>重新抛出</b>让 step / pipeline 处理</li>
 * </ul>
 */
public interface AiGateway {

    /**
     * 调 AI 一次，返回 assistant content 字符串。
     *
     * @param ctx       流水线上下文（用于 budget 扣减 / 历史记录）
     * @param systemMsg system message（通常是 stage 的 role + 指令）
     * @param userMsg   user message（数据 / 上下文）
     * @return assistant 原始 content
     * @throws AiBudgetExhaustedException 预算用完
     * @throws RuntimeException           LLM 调用失败
     */
    String call(GenerationContext ctx, String systemMsg, String userMsg);

    /**
     * 预算耗尽异常（区别于普通失败：不退币，是用户配置的高级流程问题）。
     */
    class AiBudgetExhaustedException extends RuntimeException {
        public AiBudgetExhaustedException(String msg) {
            super(msg);
        }
    }
}
