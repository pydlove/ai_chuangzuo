package com.aichuangzuo.admin.modules.commission.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CommissionAdoptRequest {
    @NotEmpty
    private List<Long> submissionIds;
}
