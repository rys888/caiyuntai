package com.caiyuntai.service.impl;

import com.caiyuntai.constant.MessageConstant;
import com.caiyuntai.constant.StatusConstant;
import com.caiyuntai.dto.BudgetDTO;
import com.caiyuntai.entity.Budget;
import com.caiyuntai.exception.BaseException;
import com.caiyuntai.mapper.BudgetMapper;
import com.caiyuntai.service.BudgetService;
import com.caiyuntai.vo.BudgetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private BudgetMapper budgetMapper;

    /**
     * 设置部门预算
     *
     * @param budgetDTO
     */
    public void save(BudgetDTO budgetDTO) {
        // 同一部门同一周期只允许一条预算记录
        Budget exist = budgetMapper.getByDeptAndPeriod(budgetDTO.getDeptId(), budgetDTO.getPeriod());
        if (exist != null) {
            throw new BaseException("该部门在该周期的预算已存在，请直接修改");
        }

        Budget budget = Budget.builder()
                .deptId(budgetDTO.getDeptId())
                .period(budgetDTO.getPeriod() == null ? currentPeriod() : budgetDTO.getPeriod())
                .totalAmount(budgetDTO.getTotalAmount() == null ? BigDecimal.ZERO : budgetDTO.getTotalAmount())
                .usedAmount(BigDecimal.ZERO)
                .status(budgetDTO.getStatus() == null ? StatusConstant.ENABLE : budgetDTO.getStatus())
                .build();

        budgetMapper.insert(budget);
    }

    /**
     * 修改预算
     *
     * @param budgetDTO
     */
    public void update(BudgetDTO budgetDTO) {
        Budget budget = Budget.builder()
                .id(budgetDTO.getId())
                .deptId(budgetDTO.getDeptId())
                .period(budgetDTO.getPeriod())
                .totalAmount(budgetDTO.getTotalAmount())
                .status(budgetDTO.getStatus())
                .build();

        budgetMapper.update(budget);
    }

    public List<BudgetVO> list(Budget budget) {
        return budgetMapper.list(budget);
    }

    public BudgetVO getVOByDeptAndPeriod(Long deptId, String period) {
        Budget budget = budgetMapper.getByDeptAndPeriod(deptId, period);
        if (budget == null) {
            return null;
        }
        return budgetMapper.getVOById(budget.getId());
    }

    /**
     * 预扣预算
     * deduct 内部是带条件的原子 UPDATE，影响行数为 0 即表示扣减失败。
     *
     * @param deptId
     * @param period
     * @param amount
     * @return
     */
    public boolean deduct(Long deptId, String period, BigDecimal amount) {
        if (deptId == null || amount == null || amount.signum() <= 0) {
            return false;
        }
        int rows = budgetMapper.deduct(deptId, period, amount);
        if (rows == 0) {
            log.warn("预算预扣失败：deptId={}, period={}, amount={}", deptId, period, amount);
            return false;
        }
        return true;
    }

    /**
     * 返还预算
     *
     * @param deptId
     * @param period
     * @param amount
     * @return
     */
    public boolean release(Long deptId, String period, BigDecimal amount) {
        if (deptId == null || amount == null) {
            return false;
        }
        int rows = budgetMapper.release(deptId, period, amount);
        if (rows == 0) {
            log.warn("预算返还失败（可能已返还过）：deptId={}, period={}, amount={}", deptId, period, amount);
            return false;
        }
        return true;
    }

    public String currentPeriod() {
        return LocalDateTime.now().format(PERIOD_FORMATTER);
    }

    public String periodOf(LocalDateTime dateTime) {
        LocalDateTime time = dateTime == null ? LocalDateTime.now() : dateTime;
        return time.format(PERIOD_FORMATTER);
    }

}
