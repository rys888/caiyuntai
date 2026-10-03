package com.caiyuntai.controller.user;

import com.caiyuntai.constant.JwtClaimsConstant;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.dto.EmployeeLoginDTO;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.properties.JwtProperties;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.DepartmentService;
import com.caiyuntai.service.EmployeeService;
import com.caiyuntai.utils.JwtUtil;
import com.caiyuntai.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 员工采购端登录接口
 * <p>
 * 改造说明：原项目这里是"微信登录"（code 换 openid）。
 * 新场景下员工端跑在 PC 浏览器上，不再有微信授权的环境，
 * 因此改为账号密码登录，身份直接复用 employee 表。
 * 路径保持 /user/user/login 不变，前端只需换端口，接口无需调整。
 */
@RestController("userUserController")
@RequestMapping("/user/user")
@Api(tags = "员工采购端-登录接口")
@Slf4j
public class UserController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 员工登录（账号密码）
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工采购端登录：{}", employeeLoginDTO.getUsername());

        //账号密码校验（任意在职员工均可登录员工端）
        Employee employee = employeeService.staffLogin(employeeLoginDTO);

        //生成员工端 jwt 令牌，claims 统一放 empId
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        // 登录成功后写入 Redis 登录状态，TTL 与 JWT 有效期一致
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstant.LOGIN_TOKEN_PREFIX + token,
                employee.getId().toString(),
                jwtProperties.getUserTtl(),
                TimeUnit.MILLISECONDS);

        EmployeeLoginVO loginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .role(employee.getRole())
                .deptId(employee.getDeptId())
                .deptName(departmentService.getNameById(employee.getDeptId()))
                .token(token)
                .build();

        return Result.success(loginVO);
    }

    /**
     * 退出登录：删除 Redis 登录状态，使旧 token 立即失效
     */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout(HttpServletRequest request) {
        String token = request.getHeader(jwtProperties.getUserTokenName());
        if (token != null && !token.isEmpty()) {
            stringRedisTemplate.delete(RedisKeyConstant.LOGIN_TOKEN_PREFIX + token);
        }
        return Result.success();
    }
}
