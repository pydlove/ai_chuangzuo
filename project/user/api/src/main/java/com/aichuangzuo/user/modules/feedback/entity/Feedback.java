package com.aichuangzuo.user.modules.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String replyContent;
    private Long replyAdminId;
    private LocalDateTime repliedAt;
    private Integer status;
    private Long tenantId;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
