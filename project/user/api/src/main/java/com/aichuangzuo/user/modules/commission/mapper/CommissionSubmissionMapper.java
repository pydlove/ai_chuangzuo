package com.aichuangzuo.user.modules.commission.mapper;

import com.aichuangzuo.user.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.user.modules.commission.vo.CommissionSubmitterVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommissionSubmissionMapper extends BaseMapper<CommissionSubmission> {

    /**
     * 统计每个任务的有效投稿数（不含已撤回）。
     *
     * @param taskIds 任务ID列表
     * @return 每个 taskId 对应的投稿数；key=taskId，value=count
     */
    @Select({
            "<script>",
            "SELECT task_id AS taskId, COUNT(*) AS cnt",
            "FROM u_commission_submission",
            "WHERE task_id IN",
            "<foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND status != 3",
            "AND is_deleted = 0",
            "GROUP BY task_id",
            "</script>"
    })
    List<Map<String, Object>> selectCountGroupByTaskId(@Param("taskIds") List<Long> taskIds);

    /**
     * 查询任务最近的有效投稿人（不含已撤回）。
     *
     * @param taskId 任务ID
     * @param limit  最大返回数
     * @return 投稿人摘要列表
     */
    @Select({
            "SELECT s.submitter_id AS submitterId, u.nickname, u.avatar_url AS avatarUrl",
            "FROM u_commission_submission s",
            "JOIN u_user u ON s.submitter_id = u.id",
            "WHERE s.task_id = #{taskId}",
            "AND s.status != 3",
            "AND s.is_deleted = 0",
            "AND u.is_deleted = 0",
            "ORDER BY s.created_at DESC",
            "LIMIT #{limit}"
    })
    List<CommissionSubmitterVO> selectSubmittersByTaskId(@Param("taskId") Long taskId, @Param("limit") int limit);
}
