package com.caiyuntai.vo;

import com.caiyuntai.entity.GoodsSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsVO implements Serializable {

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
    //规格型号（采购场景新增）
    private String spec;
    //计量单位（采购场景新增）
    private String unit;
    //0 已停用 1 已启用
    private Integer status;
    //更新时间
    private LocalDateTime updateTime;
    //分类名称
    private String categoryName;
    //物资关联的规格
    private List<GoodsSpec> flavors = new ArrayList<>();

    //private Integer copies;
}
