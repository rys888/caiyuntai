package com.caiyuntai.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门预算设置 / 修改
 */
@Data
@ApiModel(description = "预算设置传递的数据模型")
public class BudgetDTO implements Serializable {

    @ApiModelProperty("主键值（修改时传）")
    private Long id;

    @ApiModelProperty("部门id")
    private Long deptId;

    @ApiModelProperty("预算周期，格式 yyyy-MM")
    private String period;

    @ApiModelProperty("预算总额")
    private BigDecimal totalAmount;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

}
