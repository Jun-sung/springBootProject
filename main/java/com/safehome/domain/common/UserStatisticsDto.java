package com.safehome.domain.common;


import lombok.Data;
@Data
public class UserStatisticsDto {

    private Long id;
    private String email;
    private String createdDate;
    private int questionCount;

    // getter/setter
}
