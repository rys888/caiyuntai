package com.caiyuntai.service;

import com.caiyuntai.dto.ShoppingCartDTO;
import com.caiyuntai.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加申领车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看申领车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空申领车
     */
    void cleanShoppingCart();

    /**
     * 删除申领车中一个商品
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
