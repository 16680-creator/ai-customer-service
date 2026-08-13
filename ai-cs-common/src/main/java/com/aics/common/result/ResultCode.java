package com.aics.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 通用 ====================
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // ==================== 用户模块 1xxx ====================
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_ACCOUNT_DISABLED(1004, "账号已被禁用"),
    USER_TOKEN_EXPIRED(1005, "Token已过期"),
    USER_TOKEN_INVALID(1006, "Token无效"),

    // ==================== 知识库模块 2xxx ====================
    KNOWLEDGE_NOT_FOUND(2001, "知识文档不存在"),
    KNOWLEDGE_UPLOAD_FAIL(2002, "知识文档上传失败"),
    KNOWLEDGE_PARSE_FAIL(2003, "知识文档解析失败"),
    KNOWLEDGE_DUPLICATE(2004, "知识文档已存在"),

    // ==================== 对话模块 3xxx ====================
    CHAT_SESSION_NOT_FOUND(3001, "对话会话不存在"),
    CHAT_MESSAGE_SEND_FAIL(3002, "消息发送失败"),
    CHAT_AI_SERVICE_UNAVAILABLE(3003, "AI服务暂不可用"),
    CHAT_CONTEXT_OVERFLOW(3004, "对话上下文超出限制"),
    CHAT_VISION_SERVICE_UNAVAILABLE(3005, "视觉服务暂不可用，无法识别图片"),
    CHAT_IMAGE_URL_INVALID(3006, "图片地址无效或不允许访问"),

    // ==================== 搜索模块 4xxx ====================
    SEARCH_INDEX_NOT_FOUND(4001, "搜索索引不存在"),
    SEARCH_QUERY_FAIL(4002, "搜索查询失败"),
    SEARCH_INDEX_CREATE_FAIL(4003, "索引创建失败"),

    // ==================== 消息模块 5xxx ====================
    MESSAGE_SEND_FAIL(5001, "消息发送失败"),
    MESSAGE_CONSUME_FAIL(5002, "消息消费失败"),

    // ==================== 通知模块 6xxx ====================
    NOTIFY_SEND_FAIL(6001, "通知发送失败"),
    NOTIFY_WS_CONNECT_FAIL(6002, "WebSocket连接失败"),

    // ==================== 订单模块 7xxx ====================
    ORDER_STOCK_INSUFFICIENT(7001, "库存不足"),
    ORDER_CREATE_FAIL(7002, "下单失败"),
    ORDER_COUPON_UNAVAILABLE(7003, "优惠券不可用"),
    ORDER_NOT_FOUND(7004, "订单不存在或状态不允许操作"),
    ORDER_PAYMENT_METHOD_INVALID(7005, "支付方式无效"),
    ORDER_CART_EMPTY(7006, "购物车为空或未选择商品"),
    ORDER_PAY_AMOUNT_MISMATCH(7007, "支付金额与订单金额不一致"),

    // ==================== 商品模块 8xxx ====================
    PRODUCT_NOT_FOUND(8001, "商品不存在"),
    PRODUCT_OFF_SHELF(8002, "商品已下架"),
    PRODUCT_STOCK_INSUFFICIENT(8003, "商品库存不足"),
    PRODUCT_NAME_DUPLICATE(8004, "商品名称已存在"),
    PRODUCT_CATEGORY_NOT_FOUND(8005, "商品分类不存在"),
    PRODUCT_IMAGE_INVALID(8006, "商品图片格式不支持"),
    PRODUCT_IMAGE_TOO_LARGE(8007, "商品图片大小超过限制"),
    PRODUCT_INDEX_FAIL(8008, "商品向量索引失败"),
    PRODUCT_SIMILAR_SEARCH_FAIL(8009, "相似商品检索失败"),

    // ==================== 文件存储 9xxx ====================
    FILE_UPLOAD_FAIL(9001, "文件上传失败"),
    FILE_DELETE_FAIL(9002, "文件删除失败");

    private final int code;
    private final String message;
}
