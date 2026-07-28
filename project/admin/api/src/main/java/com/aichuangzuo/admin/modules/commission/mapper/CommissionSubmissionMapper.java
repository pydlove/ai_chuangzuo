package com.aichuangzuo.admin.modules.commission.mapper;

import com.aichuangzuo.admin.modules.commission.entity.CommissionSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommissionSubmissionMapper extends BaseMapper<CommissionSubmission> {

    @Select("""
            <script>
            SELECT task_id AS taskId,
                   COUNT(*) AS totalCount,
                   SUM(CASE WHEN article_biz_no LIKE 'MANUAL:%' THEN 1 ELSE 0 END) AS manualCount
            FROM u_commission_submission
            WHERE task_id IN
            <foreach collection="taskIds" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND is_deleted = 0
            GROUP BY task_id
            </script>
            """)
    List<Map<String, Object>> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
