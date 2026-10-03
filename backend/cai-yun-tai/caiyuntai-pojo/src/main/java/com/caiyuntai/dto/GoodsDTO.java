package com.caiyuntai.dto;

import com.caiyuntai.entity.GoodsSpec;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class GoodsDTO implements Serializable {

    private Long id;
    //物资名称
    private String name;
    //物资分类id
    private Long categoryId;
    //物资价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //规格型号（采购场景新增，需由管理端表单传入）
    private String spec;
    //计量单位（采购场景新增）
    private String unit;
    //0 停售 1 起售
    private Integer status;
    //规格
    private List<GoodsSpec> flavors = new ArrayList<>();

}
