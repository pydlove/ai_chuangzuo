package com.aichuangzuo.admin.modules.commission.service;

import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionBatchCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskUpdateRequest;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskImportResultVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskListVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminCommissionService {
    IPage<CommissionTaskListVO> list(String keyword, Integer status, int page, int pageSize);
    CommissionTaskDetailVO detail(Long taskId);
    Long create(CommissionTaskCreateRequest request, Long adminId);
    void update(Long taskId, CommissionTaskUpdateRequest request);
    void close(Long taskId);
    void adopt(Long taskId, List<Long> submissionIds);
    Long createSubmission(Long taskId, CommissionSubmissionCreateRequest request, Long adminId);
    int createSubmissionBatch(Long taskId, CommissionSubmissionBatchCreateRequest request, Long adminId);
    int reconcileTaskStatus();
    CommissionTaskImportResultVO importExcel(MultipartFile file, Long adminId);
    void batchDelete(List<Long> ids);
}
