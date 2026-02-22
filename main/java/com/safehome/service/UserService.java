package com.safehome.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safehome.domain.User;
import com.safehome.domain.common.UserStatisticsDto;
import com.safehome.domain.qna.Question;
import com.safehome.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	
	public String signInUser(User user) {
		
		// 중복회원 체크
		 if (userMapper.existsByEmail(user.getEmail()) > 0) {
		        return "DUPLICATE_EMAIL";
	        }
		// 비밀번호 암호화
		 String encodedPassword = passwordEncoder.encode(user.getPassword());
		 user.setPassword(encodedPassword);
		// mapper 실행
		try {
			userMapper.signInUser(user);
			return "SUCCESS";
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "ERROR";
		}
	}
	
	
	public List<UserStatisticsDto> selectAllUsers(){
		List<UserStatisticsDto> list = userMapper.selecAllUsers();
		return list;
	}
	
	public List<Question> getQuestionsByUserId(Long userId) {
        return userMapper.selectQuestionsByUserId(userId);
    }
	
	public boolean deleteUserOne(Long id) {
		return userMapper.deleteUserOne(id);
	}
	
	@Transactional
    public boolean deleteSelectedUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        int deletedCount = userMapper.deleteSelectedUsers(ids);
        return deletedCount == ids.size();
    }
	
	
	
}
