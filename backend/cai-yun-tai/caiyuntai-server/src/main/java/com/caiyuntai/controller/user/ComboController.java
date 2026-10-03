package com.caiyuntai.controller.user;

import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.ComboService;
import com.caiyuntai.vo.GoodsItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController("userComboController")
@RequestMapping("/user/combo")
@Api(tags = "C端-组合申领包浏览接口")
public class ComboController {
    @Autowired
    private ComboService comboService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 条件查询
     * <p>
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据物资分类id查询组合申领包")
    public Result<List<Combo>> list(Long categoryId) {
        // Cache-Aside：无分类时归一为 'all'，key 形如 combo:list:27 / combo:list:all
        String key = RedisKeyConstant.COMBO_LIST_PREFIX + (categoryId == null ? "all" : categoryId);
        List<Combo> list = (List<Combo>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            return Result.success(list);
        }

        Combo combo = new Combo();
        combo.setCategoryId(categoryId);
        combo.setStatus(StatusConstant.ENABLE);
        list = comboService.list(combo);

        if (list == null) {
            list = new ArrayList<>();
        }
        // 空结果短 TTL 防穿透；正常结果 30 分钟 + 随机抖动避免集中失效
        long ttl = list.isEmpty() ? 60 : (30 * 60 + ThreadLocalRandom.current().nextInt(300));
        redisTemplate.opsForValue().set(key, list, ttl, TimeUnit.SECONDS);

        return Result.success(list);
    }

    /**
     * 根据组合申领包id查询包含的物资列表
     *
     * @param id
     * @return
     */
    @GetMapping("/goods/{id}")
    @ApiOperation("根据组合申领包id查询包含的物资列表")
    public Result<List<GoodsItemVO>> goodsList(@PathVariable("id") Long id) {
        List<GoodsItemVO> list = comboService.getGoodsItemById(id);
        return Result.success(list);
    }
}
