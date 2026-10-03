package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资（原"物资"，新场景语义：企业办公物资单品）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goods implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //物资名称
    private String name;

    //物资分类id
    private Long categoryId;

    //单价
    private BigDecimal price;

    //图片
    private String image;

    //描述信息
    private String description;

    /**
     * 规格型号，例如 A4/70g/500张、12号
     * 采购场景新增字段，外卖场景不需要
     */
    private String spec;

    /**
     * 计量单位：支 / 盒 / 台 / 包
     */
    private String unit;

    //0 已停用 1 已启用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
