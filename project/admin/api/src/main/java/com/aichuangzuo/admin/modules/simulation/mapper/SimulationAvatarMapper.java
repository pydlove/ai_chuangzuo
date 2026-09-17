package com.aichuangzuo.admin.modules.simulation.mapper;

import com.aichuangzuo.admin.modules.simulation.entity.SimulationAvatar;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 模拟运营-头像库 Mapper。
 */
@Mapper
public interface SimulationAvatarMapper extends BaseMapper<SimulationAvatar> {

    /** 随机取一条（取出后由调用方按 id 删除完成"用后即删"）。 */
    @Select("SELECT id, avatar, created_at FROM a_simulation_avatar ORDER BY RAND() LIMIT 1")
    SimulationAvatar pickRandom();
}
