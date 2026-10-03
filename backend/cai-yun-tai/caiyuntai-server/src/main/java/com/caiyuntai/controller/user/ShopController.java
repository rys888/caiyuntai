package com.caiyuntai.controller.user;

import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "企业相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取企业的采购开关
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取企业的采购开关")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS);
        log.info("获取到企业的采购开关为：{}",status != null && status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
