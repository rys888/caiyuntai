package com.caiyuntai.mapper;

import com.caiyuntai.annotation.AutoFill;
import com.caiyuntai.entity.Department;
import com.caiyuntai.enumeration.OperationType;
import com.caiyuntai.vo.DepartmentVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    /**
     * 新增部门
     *
     * @param department
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Department department);

    /**
     * 动态修改部门
     *
     * @param department
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Department department);

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    @Select("select * from department where id = #{id}")
    Department getById(Long id);

    /**
     * 根据编码查询部门（用于唯一性校验）
     *
     * @param code
     * @return
     */
    @Select("select * from department where code = #{code}")
    Department getByCode(String code);

    /**
     * 删除部门
     *
     * @param id
     */
    @Delete("delete from department where id = #{id}")
    void deleteById(Long id);

    /**
     * 查询部门列表（含负责人姓名）
     *
     * @param department 动态查询条件：name / status
     * @return
     */
    List<DepartmentVO> list(Department department);

    /**
     * 查询启用状态的部门（供下拉选择）
     *
     * @return
     */
    @Select("select * from department where status = 1 order by sort asc, id asc")
    List<Department> listEnabled();

}
