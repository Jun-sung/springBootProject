package com.safehome.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String ask(String message) {
    	System.out.println("API KEY = " + apiKey);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = """
        		너는 일본 부동산 지역 추천 AI다.
        		
        		- 재해로 부터 안전한 지역을 최우선으로 추천하라.
        		- 한국어로 질문하면 한국어로 답하라.
        		- 일본어로 질문하면 일본어로 답하라.
        		- 반드시 JSON 형식으로만 답하라.
        		- 다른 설명은 절대 하지 말 것.
        		
        		{
        		  "recommendedRegions": ["지역1", "지역2", "지역3"],
        		  "reason": "추천 이유 한 줄"
        		}
        		""";

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", systemPrompt)
                        }),
                        Map.of("parts", new Object[]{
                                Map.of("text", message)
                        })
                }
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        try {
            Map candidate = (Map) ((java.util.List) response.getBody().get("candidates")).get(0);
            Map content = (Map) candidate.get("content");
            java.util.List parts = (java.util.List) content.get("parts");
            Map part = (Map) parts.get(0);
            return part.get("text").toString();
        } catch (Exception e) {
        	e.printStackTrace();
            return "AI 응답 처리 실패";
        }
    }
}