package com.aichuangzuo.admin.modules.generation.constants;

/**
 * 创作生成提示词常量池。
 *
 * <p>系统提示词（规定 AI 返回 JSON schema）由开发者维护，**不允许 admin 端修改**。
 * 这里用代码常量存放，避免 admin 误改导致输出 schema 校验失败。
 */
public final class PromptConstants {

    private PromptConstants() {
    }

    /**
     * 系统提示词：约束 AI 严格按 JSON schema 输出。
     *
     * <p>executor 拼装 system message 时，会把本常量原样附在 {@code baseContent} 之后。
     * 若需调整，开发者改这里 → 提交代码即可。
     */
    public static final String SYSTEM_PROMPT_JSON = """
            你是一名资深新媒体编辑，擅长去 AI 味的真实写作。
            请严格按 JSON 输出，**不要**输出任何 JSON 之外的文字、解释或代码块标记：

            {
              "title": "≤30 字",
              "summary": "≤80 字摘要",
              "sections": [
                { "heading": "二级标题", "paragraphs": ["段落 1", "段落 2"] }
              ],
              "imageHints": [
                { "afterSection": 1, "hint": "建议配图：xxx" }
              ],
              "meta": { "tone": "informal" }
            }

            约束：
            - sections 长度必须 3-5
            - 正文段落总字符数应围绕 wordCount
            - 严禁使用「此外」「值得注意的是」等 AI 套词
            - **字符串值内部严禁出现裸 ASCII 双引号 "**：引用他人话语或强调术语时，
              一律使用中文引号「」或『』；如必须用 ASCII 引号，转义为 \\"
            """;
}
