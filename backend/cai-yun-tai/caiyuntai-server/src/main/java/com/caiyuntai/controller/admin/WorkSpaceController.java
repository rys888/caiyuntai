package com.caiyuntai.controller.admin;

import com.caiyuntai.result.Result;
import com.caiyuntai.service.WorkspaceService;
import com.caiyuntai.vo.BusinessDataVO;
import com.caiyuntai.vo.GoodsOverViewVO;
import com.caiyuntai.vo.OrderOverViewVO;
import com.caiyuntai.vo.ComboOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作台
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
@Api(tags = "工作台相关接口")
public class WorkSpaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 工作台今日数据查询
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("工作台今日数据查询")
    public Result<BusinessDataVO> businessData(){
        //获得当天的开始时间
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        //获得当天的结束时间
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);
        return Result.success(businessDataVO);
    }

    /**
     * 查询申领单管理数据
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查询申领单管理数据")
    public Result<OrderOverViewVO> orderOverView(){
        return Result.success(workspaceService.getOrderOverView());
    }

    /**
     * 查询物资总览
     * @return
     */
    @GetMapping("/overviewGoods")
    @ApiOperation("查询物资总览")
    public Result<GoodsOverViewVO> goodsOverView(){
        return Result.success(workspaceService.getGoodsOverView());
    }

    /**
     * 查询组合申领包总览
     * @return
     */
    @GetMapping("/overviewCombos")
    @ApiOperation("查询组合申领包总览")
    public Result<ComboOverViewVO> comboOverView(){
        return Result.success(workspaceService.getComboOverView());
    }
}
