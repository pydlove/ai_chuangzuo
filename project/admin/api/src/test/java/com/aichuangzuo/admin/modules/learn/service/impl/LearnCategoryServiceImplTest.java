package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnCategoryServiceImplTest {

    @Mock
    private LearnCategoryMapper mapper;

    @InjectMocks
    private LearnCategoryServiceImpl service;

    @Test
    void tree_buildsNestedTreeOrderedBySort() {
        LearnCategoryEntity a = cat(1L, null, "A", 0);
        LearnCategoryEntity b = cat(2L, 1L, "B", 1);
        LearnCategoryEntity c = cat(3L, 1L, "C", 0);
        LearnCategoryEntity d = cat(4L, null, "D", 1);
        when(mapper.selectList(any())).thenReturn(List.of(a, b, c, d));

        List<LearnCategoryTreeNode> tree = service.tree();

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getName()).isEqualTo("A");
        assertThat(tree.get(0).getChildren())
                .extracting(LearnCategoryTreeNode::getName)
                .containsExactly("C", "B");
        assertThat(tree.get(1).getName()).isEqualTo("D");
    }

    @Test
    void create_rejectsDuplicateNameAtSameLevel() {
        LearnCategoryEntity exist = cat(99L, null, "A", 0);
        when(mapper.selectOne(any())).thenReturn(exist);

        LearnCategoryReq req = req(null, "A", 0);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(LearnErrorCode.CATEGORY_NAME_DUPLICATE.getCode());
    }

    @Test
    void delete_rejectsWhenCategoryHasChildren() {
        when(mapper.selectById(10L)).thenReturn(cat(10L, null, "X", 0));
        when(mapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(LearnErrorCode.CATEGORY_NOT_EMPTY.getCode());
    }

    private LearnCategoryEntity cat(Long id, Long parent, String name, int sort) {
        LearnCategoryEntity e = new LearnCategoryEntity();
        e.setId(id);
        e.setParentId(parent);
        e.setName(name);
        e.setSort(sort);
        return e;
    }

    private LearnCategoryReq req(Long parent, String name, int sort) {
        LearnCategoryReq r = new LearnCategoryReq();
        r.setParentId(parent);
        r.setName(name);
        r.setSort(sort);
        return r;
    }
}
