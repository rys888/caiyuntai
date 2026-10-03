package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.CategoryDTO;
import com.caiyuntai.dto.CategoryPageQueryDTO;
import com.caiyuntai.entity.Category;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 物资分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Api(tags = "物资分类相关接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增物资分类
     * @param categoryDTO
     * @return
    */
    @PostMapping
    @ApiOperation("新增物资分类")
    @RequireRole(RoleConstant.ADMIN)
    public Result<String> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增物资分类：{}", categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 物资分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("物资分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除物资分类
     * @param id
     * @return
    */
    @DeleteMapping
    @ApiOperation("删除物资分类")
    @RequireRole(RoleConstant.ADMIN)
    public Result<String> deleteById(Long id){
        log.info("删除物资分类：{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改物资分类
     * @param categoryDTO
     * @return
    */
    @PutMapping
    @ApiOperation("修改物资分类")
    @RequireRole(RoleConstant.ADMIN)
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用、禁用物资分类
     * @param status
     * @param id
     * @return
    */
    @PostMapping("/status/{status}")
    @ApiOperation("启用停用物资分类")
    @RequireRole(RoleConstant.ADMIN)
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据类型查询物资分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询物资分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
