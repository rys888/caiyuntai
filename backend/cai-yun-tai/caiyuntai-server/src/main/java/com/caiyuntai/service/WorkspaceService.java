package com.caiyuntai.service;

import com.caiyuntai.vo.BusinessDataVO;
import com.caiyuntai.vo.GoodsOverViewVO;
import com.caiyuntai.vo.OrderOverViewVO;
import com.caiyuntai.vo.ComboOverViewVO;
import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询物资总览
     * @return
     */
    GoodsOverViewVO getGoodsOverView();

    /**
     * 查询组合申领包总览
     * @return
     */
    ComboOverViewVO getComboOverView();

}
