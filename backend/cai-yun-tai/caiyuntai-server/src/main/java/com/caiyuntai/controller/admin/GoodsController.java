package com.caiyuntai.controller.admin;

import com.caiyuntai.annotation.RequireRole;
import com.caiyuntai.constant.RedisKeyConstant;
import com.caiyuntai.constant.RoleConstant;
import com.caiyuntai.dto.GoodsDTO;
import com.caiyuntai.dto.GoodsPageQueryDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.result.Result;
import com.caiyuntai.service.GoodsService;
import com.caiyuntai.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 物资管理
 */
@RestController
@RequestMapping("/admin/goods")
@Api(tags = "物资相关接口")
@Slf4j
public class GoodsController {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增物资
     *
     * @param goodsDTO
     * @return
    */
    @PostMapping
    @ApiOperation("新增物资")
    @RequireRole(RoleConstant.ADMIN)
    public Result save(@RequestBody GoodsDTO goodsDTO) {
        log.info("新增物资：{}", goodsDTO);
        goodsService.saveWithSpecs(goodsDTO);

        //清理缓存数据
        cleanCache(RedisKeyConstant.GOODS_LIST_PREFIX + "*");
        return Result.success();
    }

    /**
     * 物资分页查询
     *
     * @param goodsPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("物资分页查询")
    public Result<PageResult> page(GoodsPageQueryDTO goodsPageQueryDTO) {
        log.info("物资分页查询:{}", goodsPageQueryDTO);
        PageResult pageResult = goodsService.pageQuery(goodsPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 物资批量删除
     *
     * @param ids
     * @return
    */
    @DeleteMapping
    @ApiOperation("物资批量删除")
    @RequireRole(RoleConstant.ADMIN)
    public Result delete(@RequestParam List<Long> ids) {
        log.info("物资批量删除：{}", ids);
        goodsService.deleteBatch(ids);

        //将所有的物资缓存数据清理掉，所有以 goods:list: 开头的 key
        cleanCache(RedisKeyConstant.GOODS_LIST_PREFIX + "*");

        return Result.success();
    }

    /**
     * 根据id查询物资
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询物资")
    public Result<GoodsVO> getById(@PathVariable Long id) {
        log.info("根据id查询物资：{}", id);
        GoodsVO goodsVO = goodsService.getByIdWithSpecs(id);
        return Result.success(goodsVO);
    }

    /**
     * 修改物资
     *
     * @param goodsDTO
     * @return
    */
    @PutMapping
    @ApiOperation("修改物资")
    @RequireRole(RoleConstant.ADMIN)
    public Result update(@RequestBody GoodsDTO goodsDTO) {
        log.info("修改物资：{}", goodsDTO);
        goodsService.updateWithSpecs(goodsDTO);

        //将所有的物资缓存数据清理掉，所有以 goods:list: 开头的 key
        cleanCache(RedisKeyConstant.GOODS_LIST_PREFIX + "*");

        return Result.success();
    }

    /**
     * 物资启用停用
     *
     * @param status
     * @param id
     * @return
    */
    @PostMapping("/status/{status}")
    @ApiOperation("物资启用停用")
    @RequireRole(RoleConstant.ADMIN)
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        goodsService.startOrStop(status, id);

        //将所有的物资缓存数据清理掉，所有以 goods:list: 开头的 key
        cleanCache(RedisKeyConstant.GOODS_LIST_PREFIX + "*");

        return Result.success();
    }

    /**
     * 根据物资分类id查询物资
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据物资分类id查询物资")
    public Result<List<Goods>> list(Long categoryId) {
        List<Goods> list = goodsService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 清理缓存数据
     * @param pattern
     */
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
