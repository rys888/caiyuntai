package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申领单（原"申领单"，新场景语义：员工办公物资申领单）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 申领单状态
     * 沿用了原申领单的状态值，语义重新映射为申领流程：
     * 1 待提交(草稿，新流程下基本闲置) 2 待审批 3 采购中 4 配送中 5 已完成 6 已取消/已驳回/已退换
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;

    /**
     * 预算扣减状态（原"支付状态"）
     * 0 未扣减 1 已扣减 2 已返还
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //申领单号
    private String number;

    //申领单状态 1待提交 2待审批 3采购中 4配送中 5已完成 6已取消/已驳回/已退换
    private Integer status;

    //申请人id
    private Long userId;

    //收货地址id
    private Long addressBookId;

    //提交时间
    private LocalDateTime orderTime;

    //审批通过时间
    private LocalDateTime checkoutTime;

    //结算方式（预留，新场景下不走第三方支付）
    private Integer payMethod;

    //预算扣减状态 0未扣减 1已扣减 2已返还
    private Integer payStatus;

    //申领金额
    private BigDecimal amount;

    //申领备注
    private String remark;

    //申请人姓名
    private String userName;

    //联系电话
    private String phone;

    //收货地址
    private String address;

    //申领人
    private String consignee;

    //撤销/退换原因
    private String cancelReason;

    //审批驳回理由
    private String rejectionReason;

    //撤销时间
    private LocalDateTime cancelTime;

    //期望到货时间
    private LocalDateTime estimatedDeliveryTime;

    //配送状态  1立即送出  0选择具体时间
    private Integer deliveryStatus;

    //发货时间
    private LocalDateTime deliveryTime;

    //包装数量
    private int packAmount;

    //申领数量
    private int tablewareNumber;

    //数量状态  1按标准提供  0选择具体数量
    private Integer tablewareStatus;

    //---------------- 审批相关（新场景扩展字段） ----------------

    //审批人id
    private Long approveUser;

    //审批时间
    private LocalDateTime approveTime;

    //审批备注
    private String approveRemark;

    //申请部门id（冗余，便于按部门统计）
    private Long requesterDept;

    //最近一次催办时间
    private LocalDateTime remindTime;

    //是否退换单：0否 1是
    private Integer isReturned;
}
