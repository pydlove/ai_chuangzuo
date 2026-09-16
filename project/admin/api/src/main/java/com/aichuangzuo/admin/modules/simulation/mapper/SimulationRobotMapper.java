package com.aichuangzuo.admin.modules.simulation.mapper;

import com.aichuangzuo.admin.modules.simulation.entity.SimulationRobot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SimulationRobotMapper extends BaseMapper<SimulationRobot> {

    /**
     * 领取下一个可执行机器人：批次未完结 + 机器人到点 + 按 id 升序取第一条。
     */
    @Select("SELECT r.* FROM a_simulation_robot r "
            + "JOIN a_simulation_batch b ON b.id = r.batch_id AND b.is_deleted = 0 "
            + "WHERE r.is_deleted = 0 "
            + "AND b.status IN ('PENDING', 'RUNNING') "
            + "AND ((r.status = 'WAITING') OR (r.status = 'IN_PROGRESS' AND r.next_run_at <= #{now})) "
            + "ORDER BY r.id LIMIT 1")
    SimulationRobot selectNextExecutable(@Param("now") LocalDateTime now);

    /**
     * 原子占位：仅 WAITING 可转为 IN_PROGRESS，防多实例重复执行。
     */
    @Update("UPDATE a_simulation_robot SET status = 'IN_PROGRESS', started_at = COALESCE(started_at, #{now}), "
            + "next_run_at = #{now}, updated_at = NOW(3) "
            + "WHERE id = #{id} AND status = 'WAITING' AND is_deleted = 0")
    int claimWaiting(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 批次内用户间隔：领取一个机器人后，把同批次其余 WAITING 机器人推迟到
     * now + #{deferSeconds}，实现机器人之间的执行间隔。
     */
    @Update("UPDATE a_simulation_robot SET next_run_at = DATE_ADD(#{now}, INTERVAL #{deferSeconds} SECOND), "
            + "updated_at = NOW(3) "
            + "WHERE batch_id = #{batchId} AND status = 'WAITING' AND is_deleted = 0")
    int deferWaitingRobots(@Param("batchId") Long batchId, @Param("now") LocalDateTime now,
                           @Param("deferSeconds") int deferSeconds);

    /**
     * 取消批次下未终态机器人。
     */
    @Update("UPDATE a_simulation_robot SET status = 'CANCELED', finished_at = #{now}, updated_at = NOW(3) "
            + "WHERE batch_id = #{batchId} AND status IN ('WAITING', 'IN_PROGRESS') AND is_deleted = 0")
    int cancelOpenRobots(@Param("batchId") Long batchId, @Param("now") LocalDateTime now);

    /**
     * 统计批次下未达终态的机器人数量。
     */
    @Select("SELECT COUNT(*) FROM a_simulation_robot WHERE batch_id = #{batchId} AND is_deleted = 0 "
            + "AND status IN ('WAITING', 'IN_PROGRESS')")
    long countOpenRobots(@Param("batchId") Long batchId);

    /**
     * 所有机器人已终态但批次仍在运行的批次 ID。
     */
    @Select("SELECT b.id FROM a_simulation_batch b WHERE b.is_deleted = 0 AND b.status = 'RUNNING' "
            + "AND NOT EXISTS (SELECT 1 FROM a_simulation_robot r WHERE r.batch_id = b.id AND r.is_deleted = 0 "
            + "AND r.status IN ('WAITING', 'IN_PROGRESS'))")
    List<Long> selectRunnableBatchIdsToComplete();

    /** 所有机器人已用过的昵称（资料去重）。 */
    @Select("SELECT DISTINCT nickname FROM a_simulation_robot WHERE nickname IS NOT NULL AND nickname != ''")
    List<String> selectUsedNicknames();

    /** 所有机器人已用过的头像编号（资料去重）。 */
    @Select("SELECT DISTINCT avatar_img FROM a_simulation_robot WHERE avatar_img IS NOT NULL")
    List<Integer> selectUsedAvatarImgs();
}
