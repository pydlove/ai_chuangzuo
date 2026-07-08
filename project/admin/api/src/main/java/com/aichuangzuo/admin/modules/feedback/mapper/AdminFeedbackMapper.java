package com.aichuangzuo.admin.modules.feedback.mapper;

import com.aichuangzuo.admin.modules.feedback.entity.AdminFeedbackView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminFeedbackMapper {
    List<AdminFeedbackView> selectPage(@Param("status") Integer status,
                                       @Param("offset") long offset,
                                       @Param("size") long size);
    long countPage(@Param("status") Integer status);
    AdminFeedbackView selectById(@Param("id") Long id, @Param("status") Integer status);
    int markReplied(@Param("id") Long id,
                    @Param("replyContent") String replyContent,
                    @Param("replyAdminId") Long replyAdminId,
                    @Param("now") LocalDateTime now,
                    @Param("updatedBy") Long updatedBy);
}
