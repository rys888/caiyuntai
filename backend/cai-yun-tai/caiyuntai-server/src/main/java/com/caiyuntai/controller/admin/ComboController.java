package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.ComboDTO;
import com.caiyuntai.dto.ComboPageQueryDTO;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.ComboService;
import com.caiyuntai.vo.ComboVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 组合申领包管理
 */
@RestController
@RequestMapping("/admin/combo")
@Api(tags = "组合申领包相关接口")
@Slf4j
public class ComboController {

    @Autowired
    private ComboService comboService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增组合申领包
     *
     * @param comboDTO
     * @return
    */
    @PostMapping
    @ApiOperation("新增组合申领包")
    @RequireRole(RoleConstant.ADMIN)
    public Result save(@RequestBody ComboDTO comboDTO) {
        comboService.saveWithItems(comboDTO);
        cleanComboCache();
        return Result.success();
    }

    /**
     * 分页查询
     *
     * @param comboPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(ComboPageQueryDTO comboPageQueryDTO) {
        PageResult pageResult = comboService.pageQuery(comboPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除组合申领包
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除组合申领包")
    @RequireRole(RoleConstant.ADMIN)
    public Result delete(@RequestParam List<Long> ids) {
        comboService.deleteBatch(ids);
        cleanComboCache();
        return Result.success();
    }

    /**
     * 根据id查询组合申领包，用于修改页面回显数据
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询组合申领包")
    public Result<ComboVO> getById(@PathVariable Long id) {
        ComboVO comboVO = comboService.getByIdWithItems(id);
        return Result.success(comboVO);
    }

    /**
     * 修改组合申领包
     *
     * @param comboDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改组合申领包")
    @RequireRole(RoleConstant.ADMIN)
    public Result update(@RequestBody ComboDTO comboDTO) {
        comboService.update(comboDTO);
        cleanComboCache();
        return Result.success();
    }

    /**
     * 组合申领包启用停用
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("组合申领包启用停用")
    @RequireRole(RoleConstant.ADMIN)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        comboService.startOrStop(status, id);
        cleanComboCache();
        return Result.success();
    }

    /**
     * 清理组合申领包缓存（combo:list:*），下一次查询回源重建
     */
    private void cleanComboCache() {
        Set keys = redisTemplate.keys(RedisKeyConstant.COMBO_LIST_PREFIX + "*");
        redisTemplate.delete(keys);
    }
}
