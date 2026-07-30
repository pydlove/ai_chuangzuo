package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;

import java.util.List;

/**
 * 用户风格服务接口。
 */
public interface UserSkillService {

    /**
     * 查询当前用户的风格列表。
     *
     * @param sourceType 来源类型：1-自定义，2-学习；为空时默认 1
     * @return 风格列表，按更新时间倒序
     */
    List<UserSkillVO> listMySkills(Integer sourceType);

    /**
     * 创建自定义风格。
     *
     * @param request 创建请求
     * @return 创建后的风格视图
     */
    UserSkillVO createSkill(CreateSkillRequest request);

    /**
     * 修改当前用户的风格。
     *
     * @param bizNo   风格业务编号
     * @param request 修改请求
     * @return 更新后的风格视图
     */
    UserSkillVO updateSkill(String bizNo, UpdateSkillRequest request);

    /**
     * 删除当前用户的风格（逻辑删除）。
     *
     * @param bizNo 风格业务编号
     */
    void deleteSkill(String bizNo);

    /**
     * 增加指定用户指定名称风格的累计使用次数。
     *
     * @param userId    用户ID
     * @param skillName 风格名称
     */
    void incrementUseCount(Long userId, String skillName);
}
