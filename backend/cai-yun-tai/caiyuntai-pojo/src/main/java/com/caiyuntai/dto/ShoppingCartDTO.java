package com.caiyuntai.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDTO implements Serializable {

    private Long goodsId;
    private Long comboId;
    private String goodsSpec;

}
