package com.safehome.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safehome.domain.qna.Question;
import com.safehome.mapper.MyPageMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageMapper myPageMapper;
    private final PasswordEncoder passwordEncoder;

    // 🔹 내 질문 목록 조회
    public List<Question> findMyQuestions(Long userId) {
        return myPageMapper.findMyQuestions(userId);
    }

    // 🔹 비밀번호 변경
    @Transactional
    public void changePassword(Long userId,
                               String currentPassword,
                               String newPassword) {

        // 1. 현재 저장된 비밀번호 해시 조회
        String savedHash = myPageMapper.findPasswordHashByUserId(userId);

        if (savedHash == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, savedHash)) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 3. 새 비밀번호 암호화
        String newHash = passwordEncoder.encode(newPassword);

        // 4. DB 업데이트
        int updated = myPageMapper.updatePassword(userId, newHash);

        if (updated != 1) {
            throw new IllegalArgumentException("비밀번호 변경에 실패했습니다.");
        }
    }
}