package com.caiyuntai.controller.user;

import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.context.BaseContext;
import com.caiyuntai.dto.OrdersSubmitDTO;
import com.caiyuntai.exception.OrderBusinessException;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.OrderService;
import com.caiyuntai.vo.OrderSubmitVO;
import com.caiyuntai.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 员工采购端 - 申领单接口
 * <p>
 * 改造说明：
 * - 删除 /payment（微信支付）与对应的 OrderPaymentVO / OrdersPaymentDTO
 * - 提交申领单时后端自动预扣部门预算，前端不再需要发起支付
 * - 新增 /confirmReceipt/{id} 确认收货、/return/{id} 发起退换
 */
@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "员工采购端-申领单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 提交申领单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("提交申领单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("提交申领单，参数为：{}", ordersSubmitDTO);
        // 防重复提交：同一员工 5 秒内只允许一次提交（SET NX + TTL，不主动删除）
        Long userId = BaseContext.getCurrentId();
        String submitKey = RedisKeyConstant.ORDER_SUBMIT_PREFIX + userId;
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(submitKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            throw new OrderBusinessException(MessageConstant.ORDER_REPEAT_SUBMIT);
        }
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 我的申领单分页查询
     *
     * @param page
     * @param pageSize
     * @param status   状态 1待提交 2待审批 3采购中 4配送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("我的申领单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询申领单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询申领单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 撤销申领单
     *
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("撤销申领单")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再次申领
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再次申领")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 确认收货
     *
     * @param id
     * @return
     */
    @PutMapping("/confirmReceipt/{id}")
    @ApiOperation("确认收货")
    public Result confirmReceipt(@PathVariable("id") Long id) {
        orderService.confirmReceipt(id);
        return Result.success();
    }

    /**
     * 发起退换
     *
     * @param id
     * @param reason
     * @return
     */
    @PostMapping("/return/{id}")
    @ApiOperation("发起退换")
    public Result returnOrder(@PathVariable("id") Long id, String reason) {
        orderService.returnOrder(id, reason);
        return Result.success();
    }

    /**
     * 催办（提醒采购专员尽快审批）
     *
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催办")
    public Result reminder(@PathVariable("id") Long id) {
        orderService.reminder(id);
        return Result.success();
    }
}
