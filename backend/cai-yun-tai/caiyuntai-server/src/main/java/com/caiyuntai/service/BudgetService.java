package com.caiyuntai.service;

import com.caiyuntai.dto.BudgetDTO;
import com.caiyuntai.entity.Budget;
import com.caiyuntai.vo.BudgetVO;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService {

    /**
     * 新增/设置部门预算
     *
     * @param budgetDTO
     */
    void save(BudgetDTO budgetDTO);

    /**
     * 修改预算（主要调整总额与状态）
     *
     * @param budgetDTO
     */
    void update(BudgetDTO budgetDTO);

    /**
     * 查询预算列表
     *
     * @param budget 动态条件
     * @return
     */
    List<BudgetVO> list(Budget budget);

    /**
     * 查询某部门某周期的预算概览
     *
     * @param deptId
     * @param period
     * @return
     */
    BudgetVO getVOByDeptAndPeriod(Long deptId, String period);

    /**
     * 预扣预算。
     * 把额度判断下推到 SQL 的条件里，通过影响行数判断是否扣减成功。
     *
     * @param deptId
     * @param period
     * @param amount
     * @return true 扣减成功；false 预算不存在或余额不足
     */
    boolean deduct(Long deptId, String period, BigDecimal amount);

    /**
     * 返还预算（驳回、撤销时调用）
     *
     * @param deptId
     * @param period
     * @param amount
     * @return true 返还成功；false 无可返还额度（例如已返还过）
     */
    boolean release(Long deptId, String period, BigDecimal amount);

    /**
     * 取得当前预算周期（yyyy-MM）
     *
     * @return
     */
    String currentPeriod();

    /**
     * 按时间取预算周期（yyyy-MM）
     *
     * @param dateTime
     * @return
     */
    String periodOf(java.time.LocalDateTime dateTime);

}
