package com.caiyuntai.service.impl;

import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.dto.DepartmentDTO;
import com.caiyuntai.entity.Department;
import com.caiyuntai.exception.DeletionNotAllowedException;
import com.caiyuntai.exception.BaseException;
import com.caiyuntai.mapper.DepartmentMapper;
import com.caiyuntai.mapper.EmployeeMapper;
import com.caiyuntai.service.DepartmentService;
import com.caiyuntai.vo.DepartmentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 新增部门
     *
     * @param departmentDTO
     */
    public void save(DepartmentDTO departmentDTO) {
        // 编码唯一性校验
        Department exist = departmentMapper.getByCode(departmentDTO.getCode());
        if (exist != null) {
            throw new BaseException(MessageConstant.DEPT_CODE_EXISTS);
        }

        Department department = new Department();
        BeanUtils.copyProperties(departmentDTO, department);

        // 当前按单级部门实施，上级统一为 0
        department.setParentId(departmentDTO.getParentId() == null ? 0L : departmentDTO.getParentId());
        department.setStatus(departmentDTO.getStatus() == null ? StatusConstant.ENABLE : departmentDTO.getStatus());
        department.setSort(departmentDTO.getSort() == null ? 0 : departmentDTO.getSort());

        departmentMapper.insert(department);
    }

    /**
     * 修改部门
     *
     * @param departmentDTO
     */
    public void update(DepartmentDTO departmentDTO) {
        Department department = new Department();
        BeanUtils.copyProperties(departmentDTO, department);
        departmentMapper.update(department);
    }

    /**
     * 删除部门
     * 有员工归属的部门不允许删除，避免出现"员工挂在已删除部门上"的脏数据。
     *
     * @param id
     */
    public void deleteById(Long id) {
        Integer count = employeeMapper.countByDeptId(id);
        if (count != null && count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DEPT_HAS_EMPLOYEE);
        }
        departmentMapper.deleteById(id);
    }

    public List<DepartmentVO> list(Department department) {
        return departmentMapper.list(department);
    }

    public List<Department> listEnabled() {
        return departmentMapper.listEnabled();
    }

    public Department getById(Long id) {
        return departmentMapper.getById(id);
    }

    public String getNameById(Long id) {
        if (id == null) {
            return null;
        }
        Department department = departmentMapper.getById(id);
        return department == null ? null : department.getName();
    }

}
