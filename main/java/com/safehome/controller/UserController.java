package com.safehome.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safehome.domain.User;
import com.safehome.domain.common.UserStatisticsDto;
import com.safehome.domain.qna.Question;
import com.safehome.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
	
	@Autowired()
	private UserService userService;
	
	private final MessageSource messageSource; 
	
	@PostMapping("/signUp")
	public ResponseEntity<?> signInUser(@RequestBody User user) {
		 
		String result = userService.signInUser(user);

		    String messageKey;

		    switch (result) {
		        case "SUCCESS":
		            messageKey = "signup.success";
		            break;
		        case "DUPLICATE_EMAIL":
		            messageKey = "signup.duplicate";
		            break;
		        default:
		            messageKey = "signup.server.error";
		    }
		
		    String message = messageSource.getMessage(
	                messageKey,
	                null,
	                LocaleContextHolder.getLocale()
	        );
		    
		    if ("SUCCESS".equals(result)) {
	            return ResponseEntity.ok(message);
	        } else {
	            return ResponseEntity.badRequest().body(message);
	        }
	}
	
	// 전체회원 관리
	@GetMapping("/list")
	public List<UserStatisticsDto> selectAllUser() {
		List<UserStatisticsDto> list =  userService.selectAllUsers();
		return list;
	}
	
	// 회원별 질문 가져오기
	@GetMapping("/{userId}/questions")
	public List<Question> getUserQuestions(@PathVariable("userId") Long userId) {
	    return userService.getQuestionsByUserId(userId);
	}
	
	@DeleteMapping("/{userId}/delete")
	public boolean deleteUserOne(@PathVariable("userId") Long id) {
		boolean result =  userService.deleteUserOne(id);
		return result;
	}
	
	@DeleteMapping("/deleteSelectedUsers")
	public boolean deletSelectedUsers(@RequestBody List<Long> ids) {
	    return userService.deleteSelectedUsers(ids);

	}
	
	
	
}
