package com.safehome.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class MlitApiProps {
    @Value("${mlit.api.base-url}")
    private String baseUrl;

    @Value("${mlit.api.key}")
    private String apiKey;
}
