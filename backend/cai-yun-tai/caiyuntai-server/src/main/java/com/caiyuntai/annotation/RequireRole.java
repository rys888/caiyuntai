package com.caiyuntai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解。
 * 标注在 Controller 方法或类上，表示只有指定角色才能访问。
 * 实际校验由 RoleAspect 切面完成。
 * <p>
 * 设计取舍：未标注该注解的方法默认放行（RoleAspect 只在命中注解时才校验）。
 * 因此管理端所有「写接口」必须显式标注 @RequireRole，避免采购专员等非管理员角色越权维护基础数据；
 * 读接口可省略注解（默认放行）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色，满足其中之一即放行
     */
    String[] value();

}
