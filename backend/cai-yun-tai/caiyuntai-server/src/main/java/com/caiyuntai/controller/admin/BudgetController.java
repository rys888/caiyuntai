package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.BudgetDTO;
import com.caiyuntai.entity.Budget;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.BudgetService;
import com.caiyuntai.vo.BudgetVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门预算管理（管理端）
 * 预算的额度设置涉及资金，只允许管理员操作；采购专员可以查看以便审批时参考余额。
 * <p>
 * 注意：员工端也有一个 BudgetController，两个类名相同。
 * Spring 默认用类名（首字母小写）作为 Bean 名，会产生 ConflictingBeanDefinitionException，
 * 因此这里显式指定 Bean 名，与项目里 adminShopController / userShopController 的处理方式一致。
 */
@RestController("adminBudgetController")
@RequestMapping("/admin/budget")
@Api(tags = "预算相关接口")
@Slf4j
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    /**
     * 设置部门预算
     *
     * @param budgetDTO
     * @return
     */
    @PostMapping
    @ApiOperation("设置部门预算")
    @RequireRole(RoleConstant.ADMIN)
    public Result save(@RequestBody BudgetDTO budgetDTO) {
        log.info("设置部门预算：{}", budgetDTO);
        budgetService.save(budgetDTO);
        return Result.success();
    }

    /**
     * 修改部门预算
     *
     * @param budgetDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改部门预算")
    @RequireRole(RoleConstant.ADMIN)
    public Result update(@RequestBody BudgetDTO budgetDTO) {
        log.info("修改部门预算：{}", budgetDTO);
        budgetService.update(budgetDTO);
        return Result.success();
    }

    /**
     * 预算列表
     *
     * @param budget 动态条件：deptId / period / status
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("预算列表")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result<List<BudgetVO>> list(Budget budget) {
        return Result.success(budgetService.list(budget));
    }
}
