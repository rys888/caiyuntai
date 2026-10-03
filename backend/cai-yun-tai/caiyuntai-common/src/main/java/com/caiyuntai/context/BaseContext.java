package com.caiyuntai.context;

/**
 * 基于 ThreadLocal 的当前登录人上下文。
 * <p>
 * 拦截器在校验 JWT 通过后写入当前登录人 id 与角色，
 * Service、Mapper 切面（自动填充）等下游组件通过静态方法直接读取，
 * 避免把登录人信息一路当作方法参数往下传。
 * <p>
 * 注意：ThreadLocal 依托"一个请求由同一个线程处理"这一前提，
 * 且线程池复用线程时必须清理，否则存在读到上一个请求数据的风险，
 * 因此提供了 clear() 供拦截器在请求结束时调用。
 */
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 当前登录人角色（EMPLOYEE / PURCHASER / ADMIN）
     */
    public static ThreadLocal<String> roleThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

    public static void setCurrentRole(String role) {
        roleThreadLocal.set(role);
    }

    public static String getCurrentRole() {
        return roleThreadLocal.get();
    }

    public static void removeCurrentRole() {
        roleThreadLocal.remove();
    }

    /**
     * 一次请求结束时清理，避免线程池复用线程导致的串号问题
     */
    public static void clear() {
        threadLocal.remove();
        roleThreadLocal.remove();
    }

}
