package com.caiyuntai.controller.user;

import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.GoodsService;
import com.caiyuntai.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController("userGoodsController")
@RequestMapping("/user/goods")
@Slf4j
@Api(tags = "C端-物资浏览接口")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据物资分类id查询物资
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据物资分类id查询物资")
    public Result<List<GoodsVO>> list(Long categoryId) {
        String key = RedisKeyConstant.GOODS_LIST_PREFIX + categoryId;
        List<GoodsVO> list = (List<GoodsVO>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            return Result.success(list);
        }

        Goods goods = new Goods();
        goods.setCategoryId(categoryId);
        goods.setStatus(StatusConstant.ENABLE);

        list = goodsService.listWithSpecs(goods);
        if (list == null) {
            list = new ArrayList<>();
        }
        long ttl = list.isEmpty() ? 60 : (30 * 60 + ThreadLocalRandom.current().nextInt(300));
        redisTemplate.opsForValue().set(key, list, ttl, TimeUnit.SECONDS);

        return Result.success(list);
    }

}
