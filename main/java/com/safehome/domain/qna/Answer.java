package com.safehome.domain.qna;

import lombok.Data;

@Data
public class Answer {

    private Long id;
    private Long questionId;
    private Long adminId;
    private String content;
    private String createdAt;
}