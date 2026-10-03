package com.caiyuntai.constant;

/**
 * 员工角色常量
 * 新场景（企业办公物资采购平台）下，员工端与管理端共用一张 employee 表，
 * 靠角色区分权限。
 */
public class RoleConstant {

    /**
     * 普通员工：只能使用员工采购端，提报申领单
     */
    public static final String EMPLOYEE = "EMPLOYEE";

    /**
     * 采购专员：可审批申领单、发货、核销
     */
    public static final String PURCHASER = "PURCHASER";

    /**
     * 行政管理员：拥有全部权限，含物资目录、员工账号、预算维护
     */
    public static final String ADMIN = "ADMIN";

}
