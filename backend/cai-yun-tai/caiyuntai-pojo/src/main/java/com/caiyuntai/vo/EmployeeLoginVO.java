package com.caiyuntai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录返回的数据格式
 * 新场景下管理端与员工端共用一张 employee 表，因此两端登录返回同一结构，
 * 靠 role 区分前端可用功能。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "登录返回的数据格式")
public class EmployeeLoginVO implements Serializable {

    @ApiModelProperty("主键值")
    private Long id;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("角色：EMPLOYEE员工 / PURCHASER采购专员 / ADMIN管理员")
    private String role;

    @ApiModelProperty("所属部门id")
    private Long deptId;

    @ApiModelProperty("所属部门名称")
    private String deptName;

    @ApiModelProperty("jwt令牌")
    private String token;

}
