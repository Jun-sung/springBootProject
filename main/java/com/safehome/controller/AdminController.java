package com.safehome.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.safehome.config.CustomUserDetails;
import com.safehome.service.QnaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	private final QnaService qnaService;
	
	@GetMapping("/dashboard")
	public String dashboard() {
		return "admin/dashboard";
	}
	
    /**
     * ✅ 관리자 Q&A 목록(JSON)
     * - /admin/qna/list?page=1&size=10
     * - 응답: { list: [...], totalPages: n, currentPage: n }
     */
    @ResponseBody
    @GetMapping("/qna/list")
    public Map<String, Object> qnaList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        // QnaService.getQnaList()가 이미 페이징 계산해서 Map으로 반환
        return qnaService.getQnaList(page, size);
    }

    /**
     * ✅ 관리자 Q&A 삭제(JSON)
     * - DELETE /admin/qna/{id}
     * - 응답: true/false
     *
     * 주의:
     * - SecurityConfig에서 /admin/** 는 ADMIN만 접근 가능하도록 되어 있어야 함.
     * - Service는 deleteQuestion(id, actorUserId, isAdmin) 구조이므로 isAdmin=true로 호출.
     */
    @ResponseBody
    @DeleteMapping("/qna/{id}")
    public boolean deleteQna(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails adminUser) {

        // adminUser.getId()는 "행위자"로 전달 (로그/추적용, Service 시그니처 맞추기)
        // 관리자이므로 isAdmin=true로 고정
        return qnaService.deleteQuestion(id, adminUser.getId(), true);
    }
	
}
