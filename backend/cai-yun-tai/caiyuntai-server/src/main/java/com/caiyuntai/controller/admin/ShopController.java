package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "企业相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置企业的采购开关
     * @param status
     * @return
    */
    @PutMapping("/{status}")
    @ApiOperation("设置企业的采购开关")
    @RequireRole(RoleConstant.ADMIN)
    public Result setStatus(@PathVariable Integer status){
        log.info("设置企业的采购开关为：{}",status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(RedisKeyConstant.SHOP_STATUS,status);
        return Result.success();
    }

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
