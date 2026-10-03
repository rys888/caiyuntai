package com.caiyuntai.controller.user;

import com.caiyuntai.context.BaseContext;
import com.caiyuntai.entity.Employee;
import com.caiyuntai.mapper.EmployeeMapper;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.BudgetService;
import com.caiyuntai.vo.BudgetVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工采购端 - 预算查询
 * 员工提交申领单前需要知道本部门当月还有多少额度，
 * 前端据此做前置提示，避免提交后才报"余额不足"。
 * <p>
 * 注意：管理端也有一个 BudgetController，类名相同，故显式指定 Bean 名避免冲突。
 */
@RestController("userBudgetController")
@RequestMapping("/user/budget")
@Api(tags = "员工采购端-预算接口")
@Slf4j
public class BudgetController {

    @Autowired
    private BudgetService budgetService;
    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 查询本部门当前周期预算
     *
     * @return 预算概览，未设置预算时 data 为空
     */
    @GetMapping("/my")
    @ApiOperation("查询本部门预算")
    public Result<BudgetVO> my() {
        Long empId = BaseContext.getCurrentId();
        Employee employee = employeeMapper.getById(empId);
        if (employee == null || employee.getDeptId() == null) {
            return Result.success();
        }

        BudgetVO budgetVO = budgetService.getVOByDeptAndPeriod(employee.getDeptId(), budgetService.currentPeriod());
        return Result.success(budgetVO);
    }
}
