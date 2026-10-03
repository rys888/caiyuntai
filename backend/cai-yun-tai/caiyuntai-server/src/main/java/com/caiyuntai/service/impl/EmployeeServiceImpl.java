package com.caiyuntai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.PasswordConstant;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.context.BaseContext;
import com.caiyuntai.dto.EmployeeDTO;
import com.caiyuntai.dto.EmployeeLoginDTO;
import com.caiyuntai.dto.EmployeePageQueryDTO;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.exception.AccountLockedException;
import com.caiyuntai.exception.AccountNotFoundException;
import com.caiyuntai.exception.PasswordErrorException;
import com.caiyuntai.mapper.EmployeeMapper;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final int MAX_LOGIN_FAIL = 5;

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 员工登录（管理端）
     * 新场景下管理端与员工端共用一张 employee 表，
     * 因此这里在账号密码校验之外，还要求角色必须是采购专员或管理员。
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        Employee employee = checkLogin(employeeLoginDTO);

        //普通员工不允许登录管理端
        if (RoleConstant.EMPLOYEE.equals(employee.getRole())) {
            throw new AccountLockedException(MessageConstant.NO_ADMIN_PERMISSION);
        }

        return employee;
    }

    /**
     * 登录（员工采购端）
     * 任意在职员工都可以登录，用于提报申领单。
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee staffLogin(EmployeeLoginDTO employeeLoginDTO) {
        return checkLogin(employeeLoginDTO);
    }

    /**
     * 账号密码校验 + 账号状态校验
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee checkLogin(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        String failKey = RedisKeyConstant.LOGIN_FAIL_PREFIX + username;

        // 失败次数达到阈值，先拒绝，避免继续撞库
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_LOGIN_FAIL) {
            throw new AccountLockedException(MessageConstant.LOGIN_FAILED_TOO_MANY);
        }

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况
        if (employee == null) {
            incrLoginFail(failKey);
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            incrLoginFail(failKey);
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (StatusConstant.DISABLE.equals(employee.getStatus())) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete(failKey);
        return employee;
    }

    private void incrLoginFail(String failKey) {
        Long fails = stringRedisTemplate.opsForValue().increment(failKey, 1);
        if (fails != null && fails == 1) {
            stringRedisTemplate.expire(failKey, 15, TimeUnit.MINUTES);
        }
    }

    /**
     * 新增员工
     *
     * @param employeeDTO
     */
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);

        //设置账号的状态，默认正常状态 1表示正常 0表示锁定
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码，默认密码123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //设置当前记录的创建时间和修改时间
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());

        //设置当前记录创建人id和修改人id
        //employee.setCreateUser(BaseContext.getCurrentId());
        //employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // select * from employee limit 0,10
        //开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> records = page.getResult();
        for (Employee employee : records) {
            employee.setPassword("****");
        }

        return new PageResult(total, records);
    }

    /**
     * 启用禁用员工账号
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        // update employee set status = ? where id = ?

        /*Employee employee = new Employee();
        employee.setStatus(status);
        employee.setId(id);*/

        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();

        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工
     *
     * @param id
     * @return
     */
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("****");
        return employee;
    }

    /**
     * 编辑员工信息
     *
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        //employee.setUpdateTime(LocalDateTime.now());
        //employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.update(employee);
    }
}
