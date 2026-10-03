package com.caiyuntai.mapper;

import com.github.pagehelper.Page;
import com.caiyuntai.annotation.AutoFill;
import com.caiyuntai.dto.EmployeePageQueryDTO;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 插入员工数据
     * 新场景下 employee 增加 role / dept_id 两个字段
     * @param employee
     */
    @Insert("insert into employee (name, username, password, phone, sex, id_number, role, dept_id, create_time, update_time, create_user, update_user,status) " +
            "values " +
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{role},#{deptId},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 根据主键动态修改属性
     * @param employee
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    /**
     * 根据动态条件统计员工数量。
     * 原项目这里统计的是 user 表（微信用户）。新场景下员工端使用者就是员工，
     * user 表已废弃，因此改为统计 employee 表中的普通员工。
     * 报表/工作台仍按原方式调用，不需要改动调用方。
     *
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计某部门在职员工数
     *
     * @param deptId
     * @return
     */
    @Select("select count(id) from employee where dept_id = #{deptId} and status = 1")
    Integer countByDeptId(Long deptId);

    /**
     * 统计用户名是否已存在
     *
     * @param username
     * @return
     */
    @Select("select count(id) from employee where username = #{username}")
    Integer countByUsername(String username);

}
