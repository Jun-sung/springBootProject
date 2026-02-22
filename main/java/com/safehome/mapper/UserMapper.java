package com.safehome.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.safehome.domain.User;
import com.safehome.domain.common.UserStatisticsDto;
import com.safehome.domain.qna.Question;

@Mapper
public interface UserMapper {

	
    User findByEmail(String email);
    
    // 회원
    void signInUser(User user);
    
    int existsByEmail(String Email);
    
    List<UserStatisticsDto> selecAllUsers();
    
    List<Question> selectQuestionsByUserId(Long userId);

    //회원 한명 강제탈퇴
    boolean deleteUserOne(Long id);
    //회원 여러명 강제탈퇴
    int deleteSelectedUsers(@Param("ids") List<Long> ids);
}
