package com.caiyuntai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersCancelDTO implements Serializable {

    private Long id;
    //撤销原因
    private String cancelReason;

}
