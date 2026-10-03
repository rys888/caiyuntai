package com.caiyuntai.service;

import com.caiyuntai.dto.EmployeeDTO;
import com.caiyuntai.dto.EmployeeLoginDTO;
import com.caiyuntai.dto.EmployeePageQueryDTO;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录（管理端）
     * 只有采购专员与管理员可以登录管理端，普通员工会被拒绝。
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 登录（员工采购端）
     * 任意在职员工都可以登录员工端提报申领。
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee staffLogin(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 账号密码校验 + 账号状态校验的公共登录逻辑
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee checkLogin(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);
}
