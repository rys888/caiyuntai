package com.caiyuntai.mapper;

import com.caiyuntai.entity.GoodsSpec;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsFlavorMapper {
    /**
     * 批量插入规格数据
     * @param flavors
     */
    void insertBatch(List<GoodsSpec> flavors);

    /**
     * 根据物资id删除对应的规格数据
     * @param goodsId
     */
    @Delete("delete from dish_flavor where dish_id = #{goodsId}")
    void deleteByGoodsId(Long goodsId);

    /**
     * 根据物资id查询对应的规格数据
     * @param goodsId
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{goodsId}")
    List<GoodsSpec> getByGoodsId(Long goodsId);
}
