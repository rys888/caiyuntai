package com.caiyuntai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 上级部门id，0 表示顶级部门。
     * 当前实施按单级处理（该字段统一为 0），预留多级扩展能力。
     */
    private Long parentId;

    private String name;

    private String code;

    /**
     * 部门负责人id
     */
    private Long leaderId;

    private Integer sort;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
