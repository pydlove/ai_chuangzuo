package com.aichuangzuo.admin.modules.learn.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class LearnSortReq {

    private List<SortItem> items;

    @Data
    public static class SortItem {
        private Long id;
        private Integer sort;
        /** 仅分类拖拽时使用，文章排序时置空 */
        private Long parentId;
    }
}
