package com.caiyuntai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.context.BaseContext;
import com.caiyuntai.dto.*;
import com.caiyuntai.entity.*;
import com.caiyuntai.exception.AddressBookBusinessException;
import com.caiyuntai.exception.BudgetBusinessException;
import com.caiyuntai.exception.OrderBusinessException;
import com.caiyuntai.exception.ShoppingCartBusinessException;
import com.caiyuntai.mapper.*;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.service.BudgetService;
import com.caiyuntai.service.OrderService;
import com.caiyuntai.vo.OrderStatisticsVO;
import com.caiyuntai.vo.OrderSubmitVO;
import com.caiyuntai.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS);
        if (shopStatus != null && shopStatus == 0) {
            throw new OrderBusinessException(MessageConstant.SHOP_CLOSED);
        }

        //1. 处理各种业务异常（地址簿为空、申领车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //说明：原项目在这里调用百度地图校验"收货地址是否超出配送范围"。
        //新场景是企业内部办公物资申领，物资送到公司部门/工位，不存在配送范围概念，
        //因此该逻辑连同百度地图调用、FastJSON 解析一并移除。

        //查询当前员工的申领车数据
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList == null || shoppingCartList.size() == 0){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2. 预扣部门预算
        //   这是取代原"支付"环节的关键步骤：把额度校验下推到一条带条件的原子 UPDATE，
        //   靠数据库行锁避免并发超额申领。扣减失败即抛异常，整个事务回滚，不会产生申领单。
        Employee employee = employeeMapper.getById(userId);
        if (employee == null || employee.getDeptId() == null) {
            throw new BudgetBusinessException(MessageConstant.BUDGET_NOT_FOUND);
        }
        Long deptId = employee.getDeptId();
        BigDecimal amount = shoppingCartList.stream().map(c -> c.getAmount().multiply(BigDecimal.valueOf(c.getNumber()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        String period = budgetService.currentPeriod();

        boolean deducted = budgetService.deduct(deptId, period, amount);
        if (!deducted) {
            throw new BudgetBusinessException(MessageConstant.BUDGET_NOT_ENOUGH);
        }

        //3. 向申领单主表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setAmount(amount);
        orders.setOrderTime(LocalDateTime.now());
        //预算已经预扣成功，因此这里直接标记为已扣减
        orders.setPayStatus(Orders.PAID);
        //提交后直接进入"待审批"，去掉原流程中"待付款"这一中间态
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String numberKey = RedisKeyConstant.ORDER_NUMBER_PREFIX + dateStr;
        Long seq = stringRedisTemplate.opsForValue().increment(numberKey, 1);
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(numberKey, 2, TimeUnit.DAYS);
        }
        orders.setNumber("PO" + dateStr + "-" + String.format("%06d", seq));
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setRequesterDept(deptId);
        orders.setIsReturned(0);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //4. 向申领单明细表插入n条数据
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();//申领明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前明细关联的申领单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //5. 清空当前员工的申领车数据
        shoppingCartMapper.deleteByUserId(userId);

        //6. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 员工端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        assertOwner(orders);

        // 查询该订单对应的物资/组合申领包明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 员工撤销申领
     *
     * @param id
     */
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) throws Exception {
        // 根据id查询申领单
        Orders ordersDB = orderMapper.getById(id);

        // 校验申领单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        assertOwner(ordersDB);

        //状态 1待提交 2待审批 3采购中 4配送中 5已完成 6已取消
        //只有"待审批"之前的单据允许员工自行撤销，已进入采购流程的必须走管理员取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 撤销时返还已预扣的部门预算（带状态判断，避免重复返还）
        releaseBudget(ordersDB);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 更新申领单状态、撤销原因、撤销时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("员工撤销申领");
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
    }

    /**
     * 返还该申领单占用的部门预算。
     * <p>
     * 只在"已扣减且未返还"时执行，避免驳回、撤销、管理员取消三条路径重复返还。
     * 返还同样依赖 BudgetMapper.release 的条件 UPDATE（used_amount >= amount），
     * 即使并发重复调用也不会把已用金额刷成负数。
     *
     * @param ordersDB 申领单
     */
    private void releaseBudget(Orders ordersDB) {
        if (!Orders.PAID.equals(ordersDB.getPayStatus())) {
            // 未扣减或已返还，无需处理
            return;
        }
        Long deptId = ordersDB.getRequesterDept();
        if (deptId == null) {
            log.warn("申领单 {} 缺少申请部门，无法返还预算", ordersDB.getId());
            return;
        }
        String period = budgetService.periodOf(ordersDB.getOrderTime());
        budgetService.release(deptId, period, ordersDB.getAmount());
    }

    private void assertOwner(Orders orders) {
        if (!BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        assertOwner(ordersDB);

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为申领车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的物资信息重新复制到申领车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将申领车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单物资信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单物资信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderGoods = getOrderGoodsStr(orders);

                // 将订单物资信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderGoods(orderGoods);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取物资信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderGoodsStr(Orders orders) {
        // 查询订单物资详情信息（订单中的物资和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单物资信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderGoodsList = orderDetailList.stream().map(x -> {
            String orderGoodsItem = x.getName() + "*" + x.getNumber() + ";";
            return orderGoodsItem;
        }).collect(Collectors.toList());

        // 将该订单对应的所有物资信息拼接在一起
        return String.join("", orderGoodsList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 审批通过
     * 语义对应原项目的"接单"：只允许审批处于"待审批"状态的申领单，
     * 并把审批人与审批时间落到单据上，便于追溯。
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if (ordersDB == null || !Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .approveUser(BaseContext.getCurrentId())
                .approveTime(now)
                // 复用 checkout_time 记录审批通过时间
                .checkoutTime(now)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 审批驳回
     * 语义对应原项目的"拒单"：只允许驳回处于"待审批"状态的申领单。
     *
     * @param ordersRejectionDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询申领单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 只有存在且状态为2（待审批）才可以驳回
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 驳回需要返还已预扣的部门预算
        releaseBudget(ordersDB);

        // 更新申领单状态、驳回理由、取消时间，并记下预算已返还
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);

        orderMapper.update(orders);
    }

    /**
     * 管理员取消申领单
     *
     * @param ordersCancelDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询申领单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.COMPLETED.equals(ordersDB.getStatus()) || Orders.CANCELLED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 取消需要返还已预扣的部门预算
        releaseBudget(ordersDB);

        // 根据id更新申领单状态、取消原因、取消时间，并记下预算已返还
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 员工确认收货
     * 只有"配送中"的申领单可以确认收货，收货后单据进入已完成。
     *
     * @param id
     */
    public void confirmReceipt(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !Orders.DELIVERY_IN_PROGRESS.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 二次校验归属，避免员工确认他人的申领单
        if (!BaseContext.getCurrentId().equals(ordersDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 员工发起退换
     * 只有已完成的申领单可以发起，退换后单据置为已取消并打上退换标记。
     * 预算是否返还由管理端线下确认后处理，这里只做状态流转，避免"退换即退款"造成资金口径混乱。
     *
     * @param id
     * @param reason
     */
    public void returnOrder(Long id, String reason) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Orders.COMPLETED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_CANNOT_RETURN);
        }
        if (!BaseContext.getCurrentId().equals(ordersDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("退换：" + (reason == null ? "未填写原因" : reason))
                .cancelTime(LocalDateTime.now())
                .isReturned(1)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 员工催办（提醒采购专员尽快审批）
     * 实时推送已移除，改为在申领单上记录最近一次催办时间，
     * 管理端列表可按 remind_time 排序优先处理。
     *
     * @param id
     */
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        assertOwner(ordersDB);

        // 只有待审批的申领单才需要催办
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 催办频控：同一申领单 5 分钟内只允许成功催办一次（SET NX + TTL）
        Long userId = BaseContext.getCurrentId();
        String remindKey = RedisKeyConstant.ORDER_REMINDER_PREFIX + id;
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(remindKey, userId.toString(), 5, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            Long ttl = stringRedisTemplate.getExpire(remindKey, TimeUnit.SECONDS);
            throw new OrderBusinessException(MessageConstant.ORDER_REMINDER_TOO_FREQUENT
                    + "（剩余 " + (ttl == null ? "约 5 分钟" : ttl + " 秒") + "）");
        }

        Orders orders = Orders.builder()
                .id(id)
                .remindTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
}
