package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.user.modules.leaderboard.dto.request.IncomeSubmissionUploadRequest;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeSubmissionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 自媒体收入申报服务。
 */
public interface IncomeSubmissionService {

    /**
     * 提交收入申报。
     */
    IncomeSubmissionVO submit(Long userId, IncomeSubmissionUploadRequest request);

    /**
     * 查询当前用户的申报记录。
     */
    List<IncomeSubmissionVO> listByUser(Long userId, Integer status);

    /**
     * 上传收益截图。
     */
    List<String> uploadScreenshots(Long userId, List<MultipartFile> files);
}
