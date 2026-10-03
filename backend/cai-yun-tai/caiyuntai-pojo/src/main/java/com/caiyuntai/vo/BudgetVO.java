package com.caiyuntai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门预算概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "预算概览返回的数据格式")
public class BudgetVO implements Serializable {

    @ApiModelProperty("部门id")
    private Long deptId;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("预算周期，格式 yyyy-MM")
    private String period;

    @ApiModelProperty("预算总额")
    private BigDecimal totalAmount;

    @ApiModelProperty("已使用金额")
    private BigDecimal usedAmount;

    @ApiModelProperty("可用余额")
    private BigDecimal remainAmount;

}
