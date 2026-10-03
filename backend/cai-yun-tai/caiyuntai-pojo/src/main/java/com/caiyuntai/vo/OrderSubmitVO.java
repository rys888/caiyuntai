package com.caiyuntai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVO implements Serializable {
    //申领单id
    private Long id;
    //申领单号
    private String orderNumber;
    //申领单金额
    private BigDecimal orderAmount;
    //下单时间
    private LocalDateTime orderTime;
}
