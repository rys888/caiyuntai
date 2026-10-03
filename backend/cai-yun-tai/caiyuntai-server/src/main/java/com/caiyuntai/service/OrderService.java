package com.caiyuntai.service;

import com.caiyuntai.dto.*;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.vo.OrderStatisticsVO;
import com.caiyuntai.vo.OrderSubmitVO;
import com.caiyuntai.vo.OrderVO;

/**
 * 申领单服务
 * <p>
 * 改造说明：
 * - 原 payment / paySuccess 两个方法已删除（微信支付链路整体移除）
 * - 提交申领单时改为预扣部门预算，见 submitOrder
 * - 新增 confirmReceipt（确认收货）与 returnOrder（发起退换）
 */
public interface OrderService {

    /**
     * 提交申领单（提交即预扣部门预算，并进入待审批状态）
     *
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 员工端申领单分页查询
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 查询申领单详情
     *
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 员工撤销申领
     *
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再次申领（把原申领单明细重新写入申领车）
     *
     * @param id
     */
    void repetition(Long id);

    /**
     * 条件搜索申领单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的申领单数量统计
     *
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 审批通过
     *
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 审批驳回（返还预算）
     *
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 管理员取消申领单（返还预算）
     *
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 发货
     *
     * @param id
     */
    void delivery(Long id);

    /**
     * 核销完成
     *
     * @param id
     */
    void complete(Long id);

    /**
     * 员工确认收货
     *
     * @param id
     */
    void confirmReceipt(Long id);

    /**
     * 员工发起退换
     *
     * @param id
     * @param reason
     */
    void returnOrder(Long id, String reason);

    /**
     * 员工催办（记录催办时间）
     *
     * @param id
     */
    void reminder(Long id);
}
