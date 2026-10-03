package com.caiyuntai.mapper;

import com.github.pagehelper.Page;
import com.caiyuntai.annotation.AutoFill;
import com.caiyuntai.dto.ComboPageQueryDTO;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.enumeration.OperationType;
import com.caiyuntai.vo.GoodsItemVO;
import com.caiyuntai.vo.ComboVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ComboMapper {

    /**
     * 根据分类id查询组合申领包的数量
     *
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 根据id修改组合申领包
     *
     * @param combo
     */
    @AutoFill(OperationType.UPDATE)
    void update(Combo combo);

    /**
     * 新增组合申领包
     *
     * @param combo
     */
    @AutoFill(OperationType.INSERT)
    void insert(Combo combo);

    /**
     * 分页查询
     * @param comboPageQueryDTO
     * @return
     */
    Page<ComboVO> pageQuery(ComboPageQueryDTO comboPageQueryDTO);

    /**
     * 根据id查询组合申领包
     * @param id
     * @return
     */
    @Select("select * from setmeal where id = #{id}")
    Combo getById(Long id);

    /**
     * 根据id删除组合申领包
     * @param comboId
     */
    @Delete("delete from setmeal where id = #{id}")
    void deleteById(Long comboId);

    /**
     * 根据id查询组合申领包和组合申领包物资关系
     * @param id
     * @return
     */
    ComboVO getByIdWithItems(Long id);

    /**
     * 动态条件查询组合申领包
     * @param combo
     * @return
     */
    List<Combo> list(Combo combo);

    /**
     * 根据组合申领包id查询物资选项
     * @param comboId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{comboId}")
    List<GoodsItemVO> getGoodsItemByComboId(Long comboId);

    /**
     * 根据条件统计组合申领包数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
