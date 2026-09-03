package com.aics.common.idempotent;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;

/**
 * 幂等校验未通过（重复请求）。继承 BusinessException，
 * 由 GlobalExceptionHandler 按 409 统一渲染为标准 Result 返回体。
 */
public class IdempotentRejectException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public IdempotentRejectException(String message) {
        super(ResultCode.DUPLICATE_REQUEST, message);
    }
}
