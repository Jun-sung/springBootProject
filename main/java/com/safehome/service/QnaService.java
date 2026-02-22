package com.safehome.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.safehome.domain.qna.Question;
import com.safehome.mapper.QnaMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaMapper qnaMapper;

    public void qnaWrite(Question q) {
        qnaMapper.qnaWrite(q);
    }

    public Map<String, Object> getQnaList(int page, int size) {
        if (page < 1) page = 1;

        int offset = (page - 1) * size;

        List<Question> list = qnaMapper.loadQnaList(offset, size);
        int totalCount = qnaMapper.countQna();
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPages", totalPages);
        result.put("currentPage", page);

        return result;
    }

    public Question qnaDetail(Long id) {
        return qnaMapper.qnaDetail(id);
    }

    // ✅ Question 수정: ADMIN은 모두, USER는 본인만 (성공 여부 반환)
    public boolean updateQuestion(Long id, Long actorUserId, boolean isAdmin, String title, String content) {
        int updated = isAdmin
                ? qnaMapper.updateQuestionAsAdmin(id, title, content)
                : qnaMapper.updateQuestionAsOwner(id, actorUserId, title, content);

        return updated == 1;
    }

    // ✅ Question 삭제: ADMIN은 모두, USER는 본인만 (성공 여부 반환)
    public boolean deleteQuestion(Long id, Long actorUserId, boolean isAdmin) {
        int deleted = isAdmin
                ? qnaMapper.deleteQuestionAsAdmin(id)
                : qnaMapper.deleteQuestionAsOwner(id, actorUserId);

        return deleted == 1;
    }

    // ✅ Answer: ADMIN만 Controller에서 호출
    public boolean insertAnswer(Long questionId, Long adminId, String content) {
        return qnaMapper.insertAnswer(questionId, adminId, content) == 1;
    }

    public boolean updateAnswer(Long id, String content) {
        return qnaMapper.updateAnswer(id, content) == 1;
    }

    public boolean deleteAnswer(Long id) {
        return qnaMapper.deleteAnswer(id) == 1;
    }
}