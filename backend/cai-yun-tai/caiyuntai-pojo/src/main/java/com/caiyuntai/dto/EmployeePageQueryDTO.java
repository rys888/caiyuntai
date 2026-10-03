package com.caiyuntai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {

    //员工姓名
    private String name;

    //角色筛选：EMPLOYEE / PURCHASER / ADMIN
    private String role;

    //部门id筛选
    private Long deptId;

    //页码
    private int page;

    //每页显示记录数
    private int pageSize;

}
