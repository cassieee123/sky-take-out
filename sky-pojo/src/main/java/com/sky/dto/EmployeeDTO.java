package com.sky.dto;

import lombok.Data;

import java.io.Serializable;


/**
 *  当前端提交的数据和实体类中对应的属性差别较大时，建议使用DTO来进行封装，这里的数据字段和前端提交的数据字段都能对的上
 */
@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

}
