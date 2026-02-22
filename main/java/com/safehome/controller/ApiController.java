package com.safehome.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.safehome.config.CustomUserDetails;
import com.safehome.domain.qna.Question;
import com.safehome.service.GeminiService;
import com.safehome.service.QnaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final QnaService qnaService;
    private final GeminiService geminiService;

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return geminiService.ask(message);
    }

    // =========================
    // Question 작성 (로그인)
    // =========================
    @PostMapping("/board/write")
    public ResponseEntity<?> qnaWrite(@RequestBody Question question,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        question.setUserId(user.getId()); // ✅ 프론트에서 author/email 보내도 무시
        qnaService.qnaWrite(question);
        return ResponseEntity.ok().build();
    }

    // =========================
    // Q&A 목록
    // =========================
    @GetMapping("/qna")
    public Map<String, Object> getQnaList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        return qnaService.getQnaList(page, size);
    }

    // =========================
    // Question 수정
    // - ADMIN: 모두 가능
    // - USER: 본인 글만 가능
    // =========================
    @PutMapping("/board/update/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable("id") Long id,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean ok = qnaService.updateQuestion(id, user.getId(), isAdmin, body.get("title"), body.get("content"));

        return ok ? ResponseEntity.ok().build()
                  : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // =========================
    // Question 삭제
    // - ADMIN: 모두 가능
    // - USER: 본인 글만 가능
    // =========================
    @DeleteMapping("/board/delete/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable("id") Long id,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean ok = qnaService.deleteQuestion(id, user.getId(), isAdmin);

        return ok ? ResponseEntity.ok().build()
                  : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // =========================
    // Answer 작성 (ADMIN만)
    // =========================
    @PostMapping("/reply/write")
    public ResponseEntity<?> writeAnswer(@RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"ADMIN".equals(user.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Long questionId = Long.valueOf(body.get("postId"));
        Long adminId = user.getId();

        boolean ok = qnaService.insertAnswer(questionId, adminId, body.get("content"));
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // =========================
    // Answer 수정 (ADMIN만)
    // =========================
    @PutMapping("/reply/update/{id}")
    public ResponseEntity<?> updateAnswer(@PathVariable("id") Long id,
                                         @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"ADMIN".equals(user.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        boolean ok = qnaService.updateAnswer(id, body.get("content"));
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // =========================
    // Answer 삭제 (ADMIN만)
    // =========================
    @DeleteMapping("/reply/delete/{id}")
    public ResponseEntity<?> deleteAnswer(@PathVariable("id") Long id,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"ADMIN".equals(user.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        boolean ok = qnaService.deleteAnswer(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}