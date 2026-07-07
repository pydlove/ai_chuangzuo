package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;

import java.util.List;

/**
 * 用户风格服务接口。
 */
public interface UserStyleService {

    /**
     * 查询当前用户的风格列表。
     *
     * @param sourceType 来源类型：1-自定义，2-学习；为空时默认 1
     * @return 风格列表，按更新时间倒序
     */
    List<UserStyleVO> listMyStyles(Integer sourceType);

    /**
     * 创建自定义风格。
     *
     * @param request 创建请求
     * @return 创建后的风格视图
     */
    UserStyleVO createStyle(CreateStyleRequest request);

    /**
     * 修改当前用户的风格。
     *
     * @param bizNo   风格业务编号
     * @param request 修改请求
     * @return 更新后的风格视图
     */
    UserStyleVO updateStyle(String bizNo, UpdateStyleRequest request);

    /**
     * 删除当前用户的风格（逻辑删除）。
     *
     * @param bizNo 风格业务编号
     */
    void deleteStyle(String bizNo);
}
