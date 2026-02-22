package com.safehome.domain.qna;

import lombok.Data;

@Data
public class Question {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String createdAt;
    private Boolean answered;

    private String email; // 작성자 이메일

    // ✅ 답변(Answer)을 “평탄화”해서 담기
    private Long answerId;
    private Long answerAdminId;
    private String answerContent;
    private String answerCreatedAt;
}