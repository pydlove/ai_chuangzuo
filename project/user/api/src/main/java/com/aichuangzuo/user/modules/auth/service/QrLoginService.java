package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.dto.request.QrLoginAuthorizeRequest;
import com.aichuangzuo.user.modules.auth.dto.request.QrLoginScanRequest;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginCreateVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginStatusVO;

public interface QrLoginService {

    QrLoginCreateVO create(String clientIp, String userAgent);

    QrLoginStatusVO getStatus(String qrCode);

    QrLoginStatusVO scan(QrLoginScanRequest request, Long scannerUserId);

    AuthTokenVO authorize(QrLoginAuthorizeRequest request, String clientIp, String userAgent);

    void cancel(String qrCode);
}
