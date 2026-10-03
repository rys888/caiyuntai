package com.caiyuntai.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增 / 修改
 */
@Data
@ApiModel(description = "部门传递的数据模型")
public class DepartmentDTO implements Serializable {

    @ApiModelProperty("主键值（修改时传）")
    private Long id;

    @ApiModelProperty("上级部门id，0为顶级")
    private Long parentId;

    @ApiModelProperty("部门名称")
    private String name;

    @ApiModelProperty("部门编码")
    private String code;

    @ApiModelProperty("部门负责人id")
    private Long leaderId;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

}
