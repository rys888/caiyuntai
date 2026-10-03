package com.caiyuntai.constant;

/**
 * 信息提示常量类
 * <p>
 * 改造说明：原项目是餐饮外卖语义，新场景为「采云台」企业办公物资采购平台，
 * 因此这里的提示文案统一做了语义适配（菜品→物资、套餐→组合申领包、
 * 购物车→申领车、订单→申领单）。常量名保持不变，避免大范围改动调用方。
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了组合申领包，不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了物资，不能删除";
    public static final String SHOPPING_CART_IS_NULL = "申领车数据为空，不能提交申领单";
    public static final String ADDRESS_BOOK_IS_NULL = "收货地址为空，不能提交申领单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "组合申领包内包含已停用物资，无法启用";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "启用中的物资不能删除";
    public static final String SETMEAL_ON_SALE = "启用中的组合申领包不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前物资已被组合申领包引用，不能删除";
    public static final String ORDER_STATUS_ERROR = "申领单状态错误";
    public static final String ORDER_NOT_FOUND = "申领单不存在";

    //---------------- 新场景（企业办公物资采购平台）新增 ----------------

    public static final String LOGIN_FAILED_TOO_MANY = "登录失败次数过多，请15分钟后再试";
    public static final String SHOP_CLOSED = "采购已关闭，暂无法提交申领单";
    public static final String NO_ADMIN_PERMISSION = "当前账号无权登录管理端";
    public static final String NO_PERMISSION = "无操作权限";
    public static final String DEPT_NOT_FOUND = "部门不存在";
    public static final String DEPT_HAS_EMPLOYEE = "该部门下还有员工，不能删除";
    public static final String DEPT_CODE_EXISTS = "部门编码已存在";
    public static final String BUDGET_NOT_FOUND = "未查询到该部门的预算，请联系管理员设置";
    public static final String BUDGET_NOT_ENOUGH = "部门预算余额不足，无法提交申领单";
    public static final String BUDGET_ALREADY_RELEASED = "预算已返还，请勿重复操作";
    public static final String ORDER_CANNOT_RETURN = "当前申领单状态不支持退换";
    public static final String ORDER_REPEAT_SUBMIT = "请勿重复提交申领单";
    public static final String ORDER_REMINDER_TOO_FREQUENT = "催办过于频繁，请稍后再试";

}
