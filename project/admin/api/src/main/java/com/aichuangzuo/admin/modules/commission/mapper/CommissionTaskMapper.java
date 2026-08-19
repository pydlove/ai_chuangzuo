package com.aichuangzuo.admin.modules.commission.mapper;

import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommissionTaskMapper extends BaseMapper<CommissionTask> {
    @Select("SELECT * FROM u_commission_task WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    CommissionTask selectByIdForUpdate(Long id);

    int batchInsert(@Param("list") List<CommissionTask> tasks);

    @Update("<script>UPDATE u_commission_task SET is_deleted = 1, deleted_at = #{now}, updated_at = #{now} WHERE is_deleted = 0 AND id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}
