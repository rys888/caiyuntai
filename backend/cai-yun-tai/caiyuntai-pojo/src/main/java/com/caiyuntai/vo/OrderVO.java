package com.caiyuntai.vo;

import com.caiyuntai.entity.OrderDetail;
import com.caiyuntai.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    //申领单物资信息
    private String orderGoods;

    //申领单详情
    private List<OrderDetail> orderDetailList;

}
