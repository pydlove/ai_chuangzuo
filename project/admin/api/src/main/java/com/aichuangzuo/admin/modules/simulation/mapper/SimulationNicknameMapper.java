package com.aichuangzuo.admin.modules.simulation.mapper;

import com.aichuangzuo.admin.modules.simulation.entity.SimulationNickname;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 模拟运营-昵称库 Mapper。
 */
@Mapper
public interface SimulationNicknameMapper extends BaseMapper<SimulationNickname> {

    /** 随机取一条（取出后由调用方按 id 删除完成"用后即删"）。 */
    @Select("SELECT id, nickname, created_at FROM a_simulation_nickname ORDER BY RAND() LIMIT 1")
    SimulationNickname pickRandom();

    /** 依赖 uk_simulation_nickname 唯一索引去重；重复昵称返回 0。 */
    @Insert("INSERT IGNORE INTO a_simulation_nickname (nickname) VALUES (#{nickname})")
    int insertIgnore(@Param("nickname") String nickname);
}
