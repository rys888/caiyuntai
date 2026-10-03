package com.caiyuntai.mapper;

import com.caiyuntai.annotation.AutoFill;
import com.caiyuntai.entity.Budget;
import com.caiyuntai.enumeration.OperationType;
import com.caiyuntai.vo.BudgetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BudgetMapper {

    /**
     * 新增预算
     *
     * @param budget
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Budget budget);

    /**
     * 动态修改预算（主要改 total_amount / status）
     *
     * @param budget
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Budget budget);

    /**
     * 按部门 + 周期查询预算
     *
     * @param deptId
     * @param period
     * @return
     */
    @Select("select * from budget where dept_id = #{deptId} and period = #{period} and status = 1")
    Budget getByDeptAndPeriod(@Param("deptId") Long deptId, @Param("period") String period);

    /**
     * 按主键查询
     *
     * @param id
     * @return
     */
    @Select("select * from budget where id = #{id}")
    Budget getById(Long id);

    /**
     * 预扣预算。
     * 关键点：把"额度是否充足"的判断下推到 SQL 的 where 条件里，
     * 靠数据库行锁保证并发安全，避免"先查询再判断再更新"造成的超额扣减。
     * 调用方通过返回的影响行数判断是否扣减成功。
     *
     * @param deptId
     * @param period
     * @param amount
     * @return 影响行数：1 扣减成功；0 表示预算不存在或余额不足
     */
    int deduct(@Param("deptId") Long deptId, @Param("period") String period, @Param("amount") BigDecimal amount);

    /**
     * 返还预算。
     * 同样带 where 条件（used_amount >= amount），防止重复返还导致余额虚增。
     *
     * @param deptId
     * @param period
     * @param amount
     * @return 影响行数：1 返还成功；0 表示无可返还额度
     */
    int release(@Param("deptId") Long deptId, @Param("period") String period, @Param("amount") BigDecimal amount);

    /**
     * 预算列表（含部门名称与可用余额）
     *
     * @param budget 动态条件：deptId / period / status
     * @return
     */
    List<BudgetVO> list(Budget budget);

    /**
     * 按 id 查询预算概览
     *
     * @param id
     * @return
     */
    @Select("select b.id, b.dept_id, d.name as deptName, b.period, b.total_amount, b.used_amount, " +
            "(b.total_amount - b.used_amount) as remainAmount " +
            "from budget b left join department d on b.dept_id = d.id where b.id = #{id}")
    BudgetVO getVOById(Long id);

}
