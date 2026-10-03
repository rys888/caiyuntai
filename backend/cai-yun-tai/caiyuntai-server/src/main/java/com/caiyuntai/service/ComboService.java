package com.caiyuntai.service;

import com.caiyuntai.dto.ComboDTO;
import com.caiyuntai.dto.ComboPageQueryDTO;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.vo.GoodsItemVO;
import com.caiyuntai.vo.ComboVO;

import java.util.List;

public interface ComboService {

    /**
     * 新增组合申领包，同时需要保存组合申领包与物资的关联关系
     *
     * @param comboDTO
     */
    void saveWithItems(ComboDTO comboDTO);

    /**
     * 分页查询
     *
     * @param comboPageQueryDTO
     * @return
     */
    PageResult pageQuery(ComboPageQueryDTO comboPageQueryDTO);

    /**
     * 批量删除组合申领包
     *
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询组合申领包和关联的物资数据
     *
     * @param id
     * @return
     */
    ComboVO getByIdWithItems(Long id);

    /**
     * 修改组合申领包
     *
     * @param comboDTO
     */
    void update(ComboDTO comboDTO);

    /**
     * 组合申领包起售、停售
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 条件查询
     * @param combo
     * @return
     */
    List<Combo> list(Combo combo);

    /**
     * 根据id查询物资选项
     * @param id
     * @return
     */
    List<GoodsItemVO> getGoodsItemById(Long id);
}
