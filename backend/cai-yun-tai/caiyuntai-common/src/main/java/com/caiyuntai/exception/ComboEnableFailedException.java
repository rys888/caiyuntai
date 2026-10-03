package com.caiyuntai.exception;

/**
 * 组合申领包启用失败异常
 */
public class ComboEnableFailedException extends BaseException {

    public ComboEnableFailedException(){}

    public ComboEnableFailedException(String msg){
        super(msg);
    }
}
