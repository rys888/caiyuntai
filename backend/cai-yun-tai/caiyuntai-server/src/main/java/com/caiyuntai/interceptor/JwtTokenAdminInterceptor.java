package com.caiyuntai.interceptor;

import com.caiyuntai.constant.JwtClaimsConstant;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.context.BaseContext;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.mapper.EmployeeMapper;
import com.caiyuntai.properties.JwtProperties;
import com.caiyuntai.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理端 jwt 令牌校验拦截器
 * <p>
 * 职责边界：只负责"是否登录"——校验 JWT、把身份与角色写入 BaseContext。
 * "能不能操作"由 RoleAspect 结合 @RequireRole 判断。
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id：{}", empId);

            // 校验 Redis 登录状态：退出登录后旧 token 即使未过期也立即失效
            String loginKey = RedisKeyConstant.LOGIN_TOKEN_PREFIX + token;
            String loginValue = stringRedisTemplate.opsForValue().get(loginKey);
            if (loginValue == null) {
                response.setStatus(401);
                return false;
            }

            //3、查出角色写入上下文，供 @RequireRole 切面使用。
            //   管理端与员工端共用 employee 表，普通员工不允许进入管理端。
            Employee employee = employeeMapper.getById(empId);
            if (employee == null || RoleConstant.EMPLOYEE.equals(employee.getRole()) || StatusConstant.DISABLE.equals(employee.getStatus())) {
                response.setStatus(401);
                return false;
            }

            BaseContext.setCurrentId(empId);
            BaseContext.setCurrentRole(employee.getRole());
            //4、通过，放行
            return true;
        } catch (Exception ex) {
            //5、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 请求结束后清理 ThreadLocal。
     * Tomcat 复用线程，若不清理，下一个请求可能读到上一个请求的身份信息。
     * 这是原项目遗漏的一环（原代码只写了 remove 方法却没有调用）。
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.clear();
    }
}
