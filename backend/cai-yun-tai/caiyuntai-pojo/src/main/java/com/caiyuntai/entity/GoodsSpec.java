package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 物资规格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    //物资id
    private Long goodsId;

    //规格名
    private String name;

    //规格可选值
    private String value;

}
