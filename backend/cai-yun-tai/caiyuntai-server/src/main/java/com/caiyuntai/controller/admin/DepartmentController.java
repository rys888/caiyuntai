package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.DepartmentDTO;
import com.caiyuntai.entity.Department;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.DepartmentService;
import com.caiyuntai.vo.DepartmentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理
 * 部门属于组织架构层面的基础数据，只有管理员可以维护。
 */
@RestController
@RequestMapping("/admin/department")
@Api(tags = "部门相关接口")
@Slf4j
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 新增部门
     *
     * @param departmentDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增部门")
    @RequireRole(RoleConstant.ADMIN)
    public Result save(@RequestBody DepartmentDTO departmentDTO) {
        log.info("新增部门：{}", departmentDTO);
        departmentService.save(departmentDTO);
        return Result.success();
    }

    /**
     * 修改部门
     *
     * @param departmentDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改部门")
    @RequireRole(RoleConstant.ADMIN)
    public Result update(@RequestBody DepartmentDTO departmentDTO) {
        log.info("修改部门：{}", departmentDTO);
        departmentService.update(departmentDTO);
        return Result.success();
    }

    /**
     * 删除部门
     *
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除部门")
    @RequireRole(RoleConstant.ADMIN)
    public Result delete(Long id) {
        log.info("删除部门：{}", id);
        departmentService.deleteById(id);
        return Result.success();
    }

    /**
     * 部门列表（采购专员也需要看，用于筛选申领单）
     *
     * @param department
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("部门列表")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result<List<DepartmentVO>> list(Department department) {
        List<DepartmentVO> list = departmentService.list(department);
        return Result.success(list);
    }

    /**
     * 启用状态的部门（下拉框用）
     *
     * @return
     */
    @GetMapping("/listEnabled")
    @ApiOperation("启用部门列表")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result<List<Department>> listEnabled() {
        return Result.success(departmentService.listEnabled());
    }

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询部门")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result<Department> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }
}
