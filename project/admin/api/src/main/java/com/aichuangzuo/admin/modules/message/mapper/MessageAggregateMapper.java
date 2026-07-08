package com.aichuangzuo.admin.modules.message.mapper;

import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminVO;
import com.aichuangzuo.admin.modules.message.vo.MessageAudienceVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理端-消息管理-聚合查询 Mapper。
 *
 * <p>底层访问用户端表 {@code u_message} 与 {@code u_message_read}，
 * 设计参考 earnings/leaderboard 等模块的 aggregate mapper 风格：
 * 自定义方法用 XML 写，基础 CRUD（insert / update / selectById）继承 MyBatis-Plus BaseMapper。
 */
@Mapper
public interface MessageAggregateMapper extends BaseMapper<MessageAggregate> {

    /**
     * 列表分页查询：按 msgType 过滤，按 biz_no 聚合。
     */
    List<MessageAdminVO> selectAdminPage(@Param("msgType") String msgType,
                                         @Param("keyword") String keyword,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    /**
     * 列表总数：按 msgType 过滤，按 biz_no 去重计数。
     */
    long countAdminPage(@Param("msgType") String msgType,
                        @Param("keyword") String keyword);

    /**
     * 按 biz_no 查询 campaign 内全部 u_message 行。
     */
    List<MessageAggregate> selectByBizNo(@Param("bizNo") String bizNo);

    /**
     * 详情-受众列表（仅指定人消息有意义，广播返回空）。
     */
    List<MessageAudienceVO> selectAudienceByBizNo(@Param("bizNo") String bizNo);

    /**
     * 全体消息已读统计所需：u_user 总数（逻辑未删）。
     */
    long countAllActiveUsers();

    /**
     * 按用户写入一条消息（admin 端发反馈回复通知用，绕过 BaseMapper 的 BaseEntity 审计字段约束）。
     * content / sub_type 由 V1.0.0_019 迁移新增。
     */
    @Insert("INSERT INTO u_message (biz_no, msg_type, scope, target_user_id, title, summary, link_url, content, sub_type, tenant_id, is_deleted, created_by, updated_by) " +
            "VALUES (#{bizNo}, #{msgType}, #{scope}, #{targetUserId}, #{title}, #{summary}, #{linkUrl}, #{content}, #{subType}, #{tenantId}, 0, #{createdBy}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReply(MessageAggregate entity);
}
