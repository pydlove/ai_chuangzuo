package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.user.modules.article.dto.request.SaveDraftRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateDraftRequest;
import com.aichuangzuo.user.modules.article.vo.DraftPageVO;
import com.aichuangzuo.user.modules.article.vo.DraftVO;

/**
 * 用户草稿服务。
 */
public interface DraftService {

    /**
     * 分页查询当前用户的草稿。
     */
    DraftPageVO list(Long userId, String keyword, long page, long pageSize);

    /**
     * 查询单条草稿详情。
     */
    DraftVO get(Long userId, String bizNo);

    /**
     * 保存草稿。
     *
     * @return 新草稿的 bizNo
     */
    String save(Long userId, SaveDraftRequest request);

    /**
     * 修改草稿。
     */
    void update(Long userId, String bizNo, UpdateDraftRequest request);

    /**
     * 删除草稿。
     */
    void delete(Long userId, String bizNo);
}