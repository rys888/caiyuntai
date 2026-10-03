package com.caiyuntai.exception;

/**
 * 预算业务异常
 * 新场景下"支付"环节被"部门预算预扣/返还"取代，因此新增该异常。
 */
public class BudgetBusinessException extends BaseException {

    public BudgetBusinessException() {
    }

    public BudgetBusinessException(String msg) {
        super(msg);
    }

}
