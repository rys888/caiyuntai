package com.caiyuntai.constant;

/**
 * 统一 Redis Key 规划
 * <p>
 * 命名规范：业务:对象:标识。
 * 集中管理各业务场景的 Redis key 前缀，避免字符串散落各处。
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {}

    /** 登录状态：login:token:{token} -> employeeId */
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";

    /** 申领单防重复提交：order:submit:{employeeId} */
    public static final String ORDER_SUBMIT_PREFIX = "order:submit:";

    /** 申领单催办限流：order:reminder:{orderId} */
    public static final String ORDER_REMINDER_PREFIX = "order:reminder:";

    /** 申领单号自增：order:number:{yyyyMMdd} */
    public static final String ORDER_NUMBER_PREFIX = "order:number:";

    /** 登录失败计数：login:fail:{username} */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /** 物资列表缓存：goods:list:{categoryId} */
    public static final String GOODS_LIST_PREFIX = "goods:list:";

    /** 组合申领包列表缓存：combo:list:{categoryId|all} */
    public static final String COMBO_LIST_PREFIX = "combo:list:";

    /** 采购开关 */
    public static final String SHOP_STATUS = "shop:status";
}
