package com.safehome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    /**
     * 한국어 주석: 사용자 언어 설정을 결정하는 로직
     * SessionLocaleResolver는 사용자의 언어 선택 정보를 세션에 저장합니다.
     */
	@Bean
	public LocaleResolver localeResolver() { // 메서드 이름이 정확히 localeResolver 여야 함
	    SessionLocaleResolver slr = new SessionLocaleResolver();
	    slr.setDefaultLocale(Locale.JAPANESE); // 기본값을 일본어로 고정
	    return slr;
	}

    /**
     * 한국어 주석: 언어 변경을 감지하는 인터셉터
     * URL 뒤에 ?lang=ko 또는 ?lang=ja를 붙이면 해당 언어로 변경됩니다.
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // 파라미터 이름을 "lang"으로 지정합니다.
        lci.setParamName("lang");
        return lci;
    }

    /**
     * 한국어 주석: 설정한 인터셉터를 스프링 시스템에 등록합니다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}