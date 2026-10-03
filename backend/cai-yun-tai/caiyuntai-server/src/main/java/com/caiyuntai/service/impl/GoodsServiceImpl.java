package com.caiyuntai.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.dto.GoodsDTO;
import com.caiyuntai.dto.GoodsPageQueryDTO;
import com.caiyuntai.entity.Goods;
import com.caiyuntai.entity.GoodsSpec;
import com.caiyuntai.entity.Combo;
import com.caiyuntai.exception.DeletionNotAllowedException;
import com.caiyuntai.mapper.GoodsFlavorMapper;
import com.caiyuntai.mapper.GoodsMapper;
import com.caiyuntai.mapper.ComboDishMapper;
import com.caiyuntai.mapper.ComboMapper;
import com.caiyuntai.result.PageResult;
import com.caiyuntai.service.GoodsService;
import com.caiyuntai.vo.GoodsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private GoodsFlavorMapper goodsFlavorMapper;
    @Autowired
    private ComboDishMapper comboDishMapper;
    @Autowired
    private ComboMapper comboMapper;

    /**
     * 新增物资和对应的规格
     *
     * @param goodsDTO
     */
    @Transactional
    public void saveWithSpecs(GoodsDTO goodsDTO) {

        Goods goods = new Goods();

        BeanUtils.copyProperties(goodsDTO, goods);

        //向物资表插入1条数据
        goodsMapper.insert(goods);

        //获取insert语句生成的主键值
        Long goodsId = goods.getId();

        List<GoodsSpec> flavors = goodsDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(goodsSpec -> {
                goodsSpec.setGoodsId(goodsId);
            });
            //向规格表插入n条数据
            goodsFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 物资分页查询
     *
     * @param goodsPageQueryDTO
     * @return
     */
    public PageResult pageQuery(GoodsPageQueryDTO goodsPageQueryDTO) {
        PageHelper.startPage(goodsPageQueryDTO.getPage(), goodsPageQueryDTO.getPageSize());
        Page<GoodsVO> page = goodsMapper.pageQuery(goodsPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 物资批量删除
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前物资是否能够删除---是否存在起售中的物资？？
        for (Long id : ids) {
            Goods goods = goodsMapper.getById(id);
            if (goods.getStatus() == StatusConstant.ENABLE) {
                //当前物资处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前物资是否能够删除---是否被组合申领包关联了？？
        List<Long> comboIds = comboDishMapper.getComboIdsByGoodsIds(ids);
        if (comboIds != null && comboIds.size() > 0) {
            //当前物资被组合申领包关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除物资表中的物资数据
        for (Long id : ids) {
            goodsMapper.deleteById(id);
            //删除物资关联的规格数据
            goodsFlavorMapper.deleteByGoodsId(id);
        }
    }

    /**
     * 根据id查询物资和对应的规格数据
     *
     * @param id
     * @return
     */
    public GoodsVO getByIdWithSpecs(Long id) {
        //根据id查询物资数据
        Goods goods = goodsMapper.getById(id);

        //根据物资id查询规格数据
        List<GoodsSpec> goodsFlavors = goodsFlavorMapper.getByGoodsId(id);

        //将查询到的数据封装到VO
        GoodsVO goodsVO = new GoodsVO();
        BeanUtils.copyProperties(goods, goodsVO);
        goodsVO.setFlavors(goodsFlavors);

        return goodsVO;
    }

    /**
     * 根据id修改物资基本信息和对应的规格信息
     *
     * @param goodsDTO
     */
    @Transactional
    public void updateWithSpecs(GoodsDTO goodsDTO) {
        Goods goods = new Goods();
        BeanUtils.copyProperties(goodsDTO, goods);

        //修改物资表基本信息
        goodsMapper.update(goods);

        //删除原有的规格数据
        goodsFlavorMapper.deleteByGoodsId(goodsDTO.getId());

        //重新插入规格数据
        List<GoodsSpec> flavors = goodsDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(goodsSpec -> {
                goodsSpec.setGoodsId(goodsDTO.getId());
            });
            //向规格表插入n条数据
            goodsFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 物资起售停售
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Goods goods = Goods.builder()
                .id(id)
                .status(status)
                .build();
        goodsMapper.update(goods);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前物资的组合申领包也停售
            List<Long> goodsIds = new ArrayList<>();
            goodsIds.add(id);
            // select combo_id from combo_dish where goods_id in (?,?,?)
            List<Long> comboIds = comboDishMapper.getComboIdsByGoodsIds(goodsIds);
            if (comboIds != null && comboIds.size() > 0) {
                for (Long comboId : comboIds) {
                    Combo combo = Combo.builder()
                            .id(comboId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    comboMapper.update(combo);
                }
            }
        }
    }

    /**
     * 根据分类id查询物资
     *
     * @param categoryId
     * @return
     */
    public List<Goods> list(Long categoryId) {
        Goods goods = Goods.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return goodsMapper.list(goods);
    }

    /**
     * 条件查询物资和规格
     * @param goods
     * @return
     */
    public List<GoodsVO> listWithSpecs(Goods goods) {
        List<Goods> goodsList = goodsMapper.list(goods);

        List<GoodsVO> goodsVOList = new ArrayList<>();

        for (Goods d : goodsList) {
            GoodsVO goodsVO = new GoodsVO();
            BeanUtils.copyProperties(d,goodsVO);

            //根据物资id查询对应的规格
            List<GoodsSpec> flavors = goodsFlavorMapper.getByGoodsId(d.getId());

            goodsVO.setFlavors(flavors);
            goodsVOList.add(goodsVO);
        }

        return goodsVOList;
    }
}
