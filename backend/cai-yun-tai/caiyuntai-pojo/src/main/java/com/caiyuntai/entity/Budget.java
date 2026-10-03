package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门预算
 * 新场景下取代原项目的"支付"环节：员工提交申领单时预扣预算，
 * 审批驳回或撤销时返还。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 预算周期，格式 yyyy-MM，例如 2025-06
     */
    private String period;

    /**
     * 预算总额
     */
    private BigDecimal totalAmount;

    /**
     * 已使用金额
     */
    private BigDecimal usedAmount;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
