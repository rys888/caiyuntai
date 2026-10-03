package com.caiyuntai.aspect;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.context.BaseContext;
import com.caiyuntai.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 角色权限校验切面。
 * <p>
 * 与登录拦截器（JwtTokenAdminInterceptor / JwtTokenUserInterceptor）的分工：
 * - 拦截器负责"有没有登录"，即 JWT 是否合法，并把身份写入 BaseContext；
 * - 本切面负责"登录了能不能做这件事"，即角色是否有权限。
 * <p>
 * 之所以用切面而不是在拦截器里判断，是因为拦截器只能拿到 HandlerMethod 的类型信息，
 * 而注解的读取与方法级覆盖（类上标注、方法上覆盖）用反射处理更自然，
 * 也和项目里 AutoFillAspect 的"自定义注解 + 切面"模式保持一致。
 */
@Aspect
@Component
@Slf4j
public class RoleAspect {

    @Before("@annotation(com.caiyuntai.annotation.RequireRole) || @within(com.caiyuntai.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 方法上的注解优先，没有再看类上的
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getDeclaringClass().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return;
        }

        String[] allowed = requireRole.value();
        if (allowed.length == 0) {
            return;
        }

        String currentRole = BaseContext.getCurrentRole();
        if (currentRole == null) {
            // 没有角色信息，说明请求没有经过登录拦截器，按未登录处理
            log.warn("角色校验失败：未获取到登录角色，方法={}", method.getName());
            throw new BaseException(MessageConstant.USER_NOT_LOGIN);
        }

        boolean pass = Arrays.stream(allowed).anyMatch(role -> role.equals(currentRole));
        if (!pass) {
            log.warn("角色校验失败：当前角色={}，允许角色={}，方法={}",
                    currentRole, Arrays.toString(allowed), method.getName());
            throw new BaseException(MessageConstant.NO_PERMISSION);
        }
    }

}
