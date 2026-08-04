package com.aichuangzuo.admin.modules.article.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 管理端用户作品分页视图。
 */
@Getter
@Setter
public class AdminArticlePageVO {

    /** 作品列表。 */
    private List<AdminArticleVO> list;

    /** 总记录数。 */
    private Long total;
}
