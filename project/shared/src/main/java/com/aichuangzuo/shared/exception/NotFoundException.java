package com.aichuangzuo.shared.exception;

/**
 * 资源不存在异常；统一对外 404。
 */
public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
