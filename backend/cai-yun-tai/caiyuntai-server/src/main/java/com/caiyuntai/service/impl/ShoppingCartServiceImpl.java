package com.caiyuntai.service.impl;

import com.caiyuntai.context.BaseContext;
import com.caiyuntai.dto.ShoppingCartDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.entity.ShoppingCart;
import com.caiyuntai.mapper.GoodsMapper;
import com.caiyuntai.mapper.ComboMapper;
import com.caiyuntai.mapper.ShoppingCartMapper;
import com.caiyuntai.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.beancontext.BeanContext;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private ComboMapper comboMapper;

    /**
     * 添加申领车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前加入到申领车中的商品是否已经存在了
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果已经存在了，只需要将数量加一
        if(list != null && list.size() > 0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);//update shopping_cart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        }else {
            //如果不存在，需要插入一条申领车数据
            //判断本次添加到申领车的是物资还是组合申领包
            Long goodsId = shoppingCartDTO.getGoodsId();
            if(goodsId != null){
                //本次添加到申领车的是物资
                Goods goods = goodsMapper.getById(goodsId);
                shoppingCart.setName(goods.getName());
                shoppingCart.setImage(goods.getImage());
                shoppingCart.setAmount(goods.getPrice());
            }else{
                //本次添加到申领车的是组合申领包
                Long comboId = shoppingCartDTO.getComboId();
                Combo combo = comboMapper.getById(comboId);
                shoppingCart.setName(combo.getName());
                shoppingCart.setImage(combo.getImage());
                shoppingCart.setAmount(combo.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看申领车
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空申领车
     */
    public void cleanShoppingCart() {
        //获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除申领车中一个商品
     * @param shoppingCartDTO
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        //设置查询条件，查询当前登录用户的申领车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list != null && list.size() > 0){
            shoppingCart = list.get(0);

            Integer number = shoppingCart.getNumber();
            if(number == 1){
                //当前商品在申领车中的份数为1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }else {
                //当前商品在申领车中的份数不为1，修改份数即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }
}
