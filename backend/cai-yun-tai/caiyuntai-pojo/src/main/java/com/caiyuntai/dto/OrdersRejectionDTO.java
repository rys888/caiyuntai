package com.caiyuntai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersRejectionDTO implements Serializable {

    private Long id;

    //申领单拒绝原因
    private String rejectionReason;

}
