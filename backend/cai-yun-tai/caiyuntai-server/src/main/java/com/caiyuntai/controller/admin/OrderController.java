package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.OrdersCancelDTO;
import com.caiyuntai.dto.OrdersConfirmDTO;
import com.caiyuntai.dto.OrdersPageQueryDTO;
import com.caiyuntai.dto.OrdersRejectionDTO;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.OrderService;
import com.caiyuntai.vo.OrderStatisticsVO;
import com.caiyuntai.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 申领单管理
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "申领单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 申领单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("申领单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的申领单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的申领单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 申领单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询申领单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 审批通过
     *
     * @return
    */
    @PutMapping("/confirm")
    @ApiOperation("审批通过")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 审批驳回
     *
     * @return
    */
    @PutMapping("/rejection")
    @ApiOperation("审批驳回")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消申领单
     *
     * @return
    */
    @PutMapping("/cancel")
    @ApiOperation("取消申领单")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送申领单
     *
     * @return
    */
    @PutMapping("/delivery/{id}")
    @ApiOperation("发货")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成申领单
     *
     * @return
    */
    @PutMapping("/complete/{id}")
    @ApiOperation("核销完成")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.PURCHASER})
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }
}
