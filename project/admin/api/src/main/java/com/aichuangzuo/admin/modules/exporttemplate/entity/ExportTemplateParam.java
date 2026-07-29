package com.aichuangzuo.admin.modules.exporttemplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 导出模板「参数定义」实体，对应表 {@code a_export_template_param}。
 *
 * <p>把 visual_style_json 的字段拆成可命名的参数：
 * admin 端按 field_type 渲染对应控件（color/number/text/select/border）。
 *
 * <p>这是一次定义、30 个模板共享的 schema 表；
 * 每个模板的具体参数值仍存 a_export_template.visual_style_json。
 */
@Getter
@Setter
@TableName("a_export_template_param")
public class ExportTemplateParam {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数 key，对应 visual_style_json 字段名。 */
    private String paramKey;

    /** admin 端显示名。 */
    private String displayLabel;

    /** 控件类型：color / number / text / select / border。 */
    private String fieldType;

    /** 分组：标题/Meta/正文/小标题/高亮块/整体/排版。 */
    private String groupLabel;

    /** 新建模板时填入 visual_style_json 的默认值。 */
    private String defaultValue;

    /** select 类型的可选项 JSON 数组字符串。 */
    private String optionsJson;

    private Integer sortOrder;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}