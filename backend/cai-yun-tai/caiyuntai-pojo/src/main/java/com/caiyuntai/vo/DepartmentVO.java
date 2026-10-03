package com.caiyuntai.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门信息（含负责人姓名，便于前端直接展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "部门信息返回的数据格式")
public class DepartmentVO implements Serializable {

    @ApiModelProperty("主键值")
    private Long id;

    @ApiModelProperty("上级部门id，0为顶级")
    private Long parentId;

    @ApiModelProperty("部门名称")
    private String name;

    @ApiModelProperty("部门编码")
    private String code;

    @ApiModelProperty("部门负责人id")
    private Long leaderId;

    @ApiModelProperty("部门负责人姓名")
    private String leaderName;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

}
