package com.safehome.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.safehome.domain.qna.Question;
import com.safehome.service.QnaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

	private final QnaService qnaService;
	
	@GetMapping("/")
	public String index(HttpServletRequest request, Model model) {
	    model.addAttribute("currentUri", request.getRequestURI());
	    return "index";
	}

    @GetMapping("/qna")
    public String qna(HttpServletRequest request, Model model) {
	    model.addAttribute("currentUri", request.getRequestURI());
    	return "/qna/qna"; 
    }
    
    @GetMapping("/qna/detail/{id}")
    public String qnaDetail(HttpServletRequest request, Model model,@PathVariable("id") Long id) {
    	Question result = qnaService.qnaDetail(id);
    	model.addAttribute("currentUri", request.getRequestURI());
    	model.addAttribute("post",result);
    	return "/qna/detail";  
    }
    
    @GetMapping("/map")
    public String mapPage(@RequestParam(name="region", required = false, defaultValue = "tokyo") String region,
                          Model model,HttpServletRequest request) {
        model.addAttribute("region", region); // map.html에서 초기 region 사용
    	model.addAttribute("currentUri", request.getRequestURI());

        return "land/map"; // templates/land/map.html
    }
    
    
    
    
}