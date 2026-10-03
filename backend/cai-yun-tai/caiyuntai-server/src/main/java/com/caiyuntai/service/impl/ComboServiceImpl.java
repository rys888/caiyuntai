package com.caiyuntai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.dto.ComboDTO;
import com.caiyuntai.dto.ComboPageQueryDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.entity.ComboItem;
import com.caiyuntai.exception.DeletionNotAllowedException;
import com.caiyuntai.exception.ComboEnableFailedException;
import com.caiyuntai.mapper.GoodsMapper;
import com.caiyuntai.mapper.ComboDishMapper;
import com.caiyuntai.mapper.ComboMapper;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.service.ComboService;
import com.caiyuntai.vo.GoodsItemVO;
import com.caiyuntai.vo.ComboVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组合申领包业务实现
 */
@Service
@Slf4j
public class ComboServiceImpl implements ComboService {

    @Autowired
    private ComboMapper comboMapper;
    @Autowired
    private ComboDishMapper comboDishMapper;
    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 新增组合申领包，同时需要保存组合申领包与物资的关联关系
     *
     * @param comboDTO
     */
    @Transactional
    public void saveWithItems(ComboDTO comboDTO) {
        Combo combo = new Combo();
        BeanUtils.copyProperties(comboDTO, combo);

        //向组合申领包表插入数据
        comboMapper.insert(combo);

        //获取生成的组合申领包id
        Long comboId = combo.getId();

        List<ComboItem> comboItems = comboDTO.getComboItems();
        comboItems.forEach(comboDish -> {
            comboDish.setComboId(comboId);
        });

        //保存组合申领包与物资的关联关系
        comboDishMapper.insertBatch(comboItems);
    }

    /**
     * 分页查询
     *
     * @param comboPageQueryDTO
     * @return
     */
    public PageResult pageQuery(ComboPageQueryDTO comboPageQueryDTO) {
        int pageNum = comboPageQueryDTO.getPage();
        int pageSize = comboPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<ComboVO> page = comboMapper.pageQuery(comboPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除组合申领包
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            Combo combo = comboMapper.getById(id);
            if (StatusConstant.ENABLE == combo.getStatus()) {
                //起售中的组合申领包不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(comboId -> {
            //删除组合申领包表中的数据
            comboMapper.deleteById(comboId);
            //删除组合申领包物资关系表中的数据
            comboDishMapper.deleteByComboId(comboId);
        });
    }

    /**
     * 根据id查询组合申领包和组合申领包物资关系
     *
     * @param id
     * @return
     */
    public ComboVO getByIdWithItems(Long id) {
        ComboVO comboVO = comboMapper.getByIdWithItems(id);
        return comboVO;
    }

    /**
     * 修改组合申领包
     *
     * @param comboDTO
     */
    @Transactional
    public void update(ComboDTO comboDTO) {
        Combo combo = new Combo();
        BeanUtils.copyProperties(comboDTO, combo);

        //1、修改组合申领包表，执行update
        comboMapper.update(combo);

        //组合申领包id
        Long comboId = comboDTO.getId();

        //2、删除组合申领包与物资的关联关系，操作setmeal_dish表，执行delete
        comboDishMapper.deleteByComboId(comboId);

        List<ComboItem> comboItems = comboDTO.getComboItems();
        comboItems.forEach(comboDish -> {
            comboDish.setComboId(comboId);
        });
        //3、重新插入组合申领包与物资的关联关系，操作setmeal_dish表，执行insert
        comboDishMapper.insertBatch(comboItems);
    }

    /**
     * 组合申领包启用、停用
     *
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //启用组合包时，判断包内是否有已停用的物资，有则提示"组合申领包内包含已停用物资，无法启用"
        if (status == StatusConstant.ENABLE) {
            //select a.* from goods a left join combo_dish b on a.id = b.goods_id where b.combo_id = ?
            List<Goods> goodsList = goodsMapper.getByComboId(id);
            if (goodsList != null && goodsList.size() > 0) {
                goodsList.forEach(goods -> {
                    if (StatusConstant.DISABLE == goods.getStatus()) {
                        throw new ComboEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Combo combo = Combo.builder()
                .id(id)
                .status(status)
                .build();
        comboMapper.update(combo);
    }

    /**
     * 条件查询
     * @param combo
     * @return
     */
    public List<Combo> list(Combo combo) {
        List<Combo> list = comboMapper.list(combo);
        return list;
    }

    /**
     * 根据id查询物资选项
     * @param id
     * @return
     */
    public List<GoodsItemVO> getGoodsItemById(Long id) {
        return comboMapper.getGoodsItemByComboId(id);
    }
}
