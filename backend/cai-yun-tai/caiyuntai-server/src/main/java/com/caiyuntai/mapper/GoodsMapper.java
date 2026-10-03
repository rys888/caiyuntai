package com.caiyuntai.mapper;

import com.github.pagehelper.Page;
import com.caiyuntai.annotation.AutoFill;
import com.caiyuntai.dto.GoodsPageQueryDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.enumeration.OperationType;
import com.caiyuntai.vo.GoodsVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GoodsMapper {

    /**
     * 根据分类id查询物资数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入物资数据
     *
     * @param goods
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Goods goods);

    /**
     * 物资分页查询
     *
     * @param goodsPageQueryDTO
     * @return
     */
    Page<GoodsVO> pageQuery(GoodsPageQueryDTO goodsPageQueryDTO);

    /**
     * 根据主键查询物资
     *
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Goods getById(Long id);

    /**
     * 根据主键删除物资数据
     *
     * @param id
     */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id动态修改物资数据
     *
     * @param goods
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Goods goods);

    /**
     * 动态条件查询物资
     *
     * @param goods
     * @return
     */
    List<Goods> list(Goods goods);

    /**
     * 根据组合申领包id查询物资
     * @param comboId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{comboId}")
    List<Goods> getByComboId(Long comboId);

    /**
     * 根据条件统计物资数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
