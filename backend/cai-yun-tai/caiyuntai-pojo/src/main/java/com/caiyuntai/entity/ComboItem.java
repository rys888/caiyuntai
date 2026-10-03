package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合申领包与物资关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //组合申领包id
    private Long comboId;

    //物资id
    private Long goodsId;

    //物资名称 （冗余字段）
    private String name;

    //物资单价
    private BigDecimal price;

    //份数
    private Integer copies;
}
