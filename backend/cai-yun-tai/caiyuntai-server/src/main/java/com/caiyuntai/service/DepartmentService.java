package com.caiyuntai.service;

import com.caiyuntai.dto.DepartmentDTO;
import com.caiyuntai.entity.Department;
import com.caiyuntai.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    /**
     * 新增部门
     *
     * @param departmentDTO
     */
    void save(DepartmentDTO departmentDTO);

    /**
     * 修改部门
     *
     * @param departmentDTO
     */
    void update(DepartmentDTO departmentDTO);

    /**
     * 删除部门（有员工归属时不允许删除）
     *
     * @param id
     */
    void deleteById(Long id);

    /**
     * 查询部门列表
     *
     * @param department 动态条件：name / status
     * @return
     */
    List<DepartmentVO> list(Department department);

    /**
     * 查询启用状态的部门（下拉框用）
     *
     * @return
     */
    List<Department> listEnabled();

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    Department getById(Long id);

    /**
     * 根据id查询部门名称（登录返回时补齐部门名，id 为空时返回 null）
     *
     * @param id
     * @return
     */
    String getNameById(Long id);

}
