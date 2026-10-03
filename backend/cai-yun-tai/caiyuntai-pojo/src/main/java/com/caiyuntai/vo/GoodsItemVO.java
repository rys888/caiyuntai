package com.caiyuntai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsItemVO implements Serializable {

    //物资名称
    private String name;

    //份数
    private Integer copies;

    //物资图片
    private String image;

    //物资描述
    private String description;
}
