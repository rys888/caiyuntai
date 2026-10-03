package com.caiyuntai.service;

import com.caiyuntai.dto.GoodsDTO;
import com.caiyuntai.dto.GoodsPageQueryDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.vo.GoodsVO;

import java.util.List;

public interface GoodsService {

    /**
     * 新增物资和对应的规格
     *
     * @param goodsDTO
     */
    public void saveWithSpecs(GoodsDTO goodsDTO);

    /**
     * 物资分页查询
     *
     * @param goodsPageQueryDTO
     * @return
     */
    PageResult pageQuery(GoodsPageQueryDTO goodsPageQueryDTO);

    /**
     * 物资批量删除
     *
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询物资和对应的规格数据
     *
     * @param id
     * @return
     */
    GoodsVO getByIdWithSpecs(Long id);

    /**
     * 根据id修改物资基本信息和对应的规格信息
     *
     * @param goodsDTO
     */
    void updateWithSpecs(GoodsDTO goodsDTO);

    /**
     * 物资起售停售
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据分类id查询物资
     *
     * @param categoryId
     * @return
     */
    List<Goods> list(Long categoryId);

    /**
     * 条件查询物资和规格
     * @param goods
     * @return
     */
    List<GoodsVO> listWithSpecs(Goods goods);
}
