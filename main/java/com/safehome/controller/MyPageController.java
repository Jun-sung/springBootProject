package com.safehome.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.safehome.config.CustomUserDetails;
import com.safehome.domain.qna.Question;
import com.safehome.service.MyPageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MyPageController {

	@Autowired
	private MessageSource messageSource;
    private final MyPageService myPageService;

    @GetMapping("/mypage")
    public String mypage(HttpServletRequest request,
    					@RequestParam(name="tab", defaultValue = "profile") String tab,
                         @AuthenticationPrincipal CustomUserDetails user,
                         Model model) {
    	model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("tab", tab);
        model.addAttribute("userEmail", user.getUsername());

        if ("questions".equals(tab)) {
            List<Question> myQuestions = myPageService.findMyQuestions(user.getId());
            model.addAttribute("myQuestions", myQuestions);
        }

        // favorites 탭은 추후 myPageService.findMyFavorites(user.getId())로 붙이면 됨

        return "/user/mypage";
    }

    @PostMapping("/mypage/password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam(name="currentPassword") String currentPassword,
                                 @RequestParam(name="newPassword") String newPassword,
                                 @RequestParam(name="confirmPassword") String confirmPassword,
                                 RedirectAttributes ra,Locale locale) {
    	String errorMessage = messageSource.getMessage("mypage.password.change.err", null, locale);
    	String success = messageSource.getMessage("mypage.password.change.msg", null, locale);
    	
    	
    	
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("err", errorMessage);
            
            return "redirect:/mypage?tab=profile";
        }

        try {
            myPageService.changePassword(user.getId(), currentPassword, newPassword);
            ra.addFlashAttribute("msg", success);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("err", errorMessage);
        }

        return "redirect:/mypage?tab=profile";
    }
}