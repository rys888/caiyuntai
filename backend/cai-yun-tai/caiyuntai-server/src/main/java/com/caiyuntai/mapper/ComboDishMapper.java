package com.caiyuntai.mapper;

import com.caiyuntai.entity.ComboItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ComboDishMapper {
    /**
     * 根据物资id查询对应的组合申领包id
     *
     * @param goodsIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getComboIdsByGoodsIds(List<Long> goodsIds);

    /**
     * 批量保存组合申领包与物资的关联关系
     *
     * @param comboItems
     */
    void insertBatch(List<ComboItem> comboItems);

    /**
     * 根据组合申领包id删除组合申领包与物资的关联关系
     *
     * @param comboId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{comboId}")
    void deleteByComboId(Long comboId);
}
