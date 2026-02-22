package com.safehome.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.safehome.domain.qna.Question;

@Mapper
public interface QnaMapper {

    // 목록
    List<Question> loadQnaList(@Param("offset") int offset, @Param("size") int size);
    int countQna();

    // 상세(질문 + 작성자 email + 답변 join)
    Question qnaDetail(@Param("id") Long id);

    // 질문 작성
    int qnaWrite(Question q);

    // 작성자 확인용
    Long findQuestionOwnerId(@Param("id") Long id);

    // 질문 수정/삭제 (ADMIN)
    int updateQuestionAsAdmin(@Param("id") Long id,
                              @Param("title") String title,
                              @Param("content") String content);

    int deleteQuestionAsAdmin(@Param("id") Long id);

    // 질문 수정/삭제 (OWNER)
    int updateQuestionAsOwner(@Param("id") Long id,
                              @Param("userId") Long userId,
                              @Param("title") String title,
                              @Param("content") String content);

    int deleteQuestionAsOwner(@Param("id") Long id,
                              @Param("userId") Long userId);

    // 답변 (ADMIN 전용으로 컨트롤러/시큐리티에서 제한)
    int insertAnswer(@Param("questionId") Long questionId,
                     @Param("adminId") Long adminId,
                     @Param("content") String content);

    int updateAnswer(@Param("id") Long id,
                     @Param("content") String content);

    int deleteAnswer(@Param("id") Long id);
}