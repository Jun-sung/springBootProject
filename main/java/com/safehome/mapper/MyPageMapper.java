package com.safehome.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.safehome.domain.qna.Question;

@Mapper
public interface MyPageMapper {

    List<Question> findMyQuestions(@Param("userId") Long userId);

    String findPasswordHashByUserId(@Param("userId") Long userId);

    int updatePassword(@Param("userId") Long userId, @Param("newHash") String newHash);
    
    
}