package com.aichuangzuo.admin.modules.exporttemplate.vo;

import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplateParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

/**
 * 参数定义 VO：把 optionsJson 字符串反序列化成 List<String> 给前端直接用。
 */
@Data
public class ExportTemplateParamVO {

    private Long id;
    private String paramKey;
    private String displayLabel;
    private String fieldType;
    private String groupLabel;
    private String defaultValue;
    private List<String> options;
    private Integer sortOrder;

    public static ExportTemplateParamVO from(ExportTemplateParam p, ObjectMapper mapper) {
        ExportTemplateParamVO vo = new ExportTemplateParamVO();
        vo.setId(p.getId());
        vo.setParamKey(p.getParamKey());
        vo.setDisplayLabel(p.getDisplayLabel());
        vo.setFieldType(p.getFieldType());
        vo.setGroupLabel(p.getGroupLabel());
        vo.setDefaultValue(p.getDefaultValue());
        vo.setSortOrder(p.getSortOrder());
        if (p.getOptionsJson() != null && !p.getOptionsJson().isBlank()) {
            try {
                List<String> opts = mapper.readValue(p.getOptionsJson(),
                        mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                vo.setOptions(opts);
            } catch (Exception ignored) {
                vo.setOptions(null);
            }
        }
        return vo;
    }
}