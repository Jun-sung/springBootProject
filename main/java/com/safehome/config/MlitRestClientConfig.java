package com.safehome.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.safehome.util.MlitApiProps;

@Configuration
public class MlitRestClientConfig {

    @Bean
    public RestClient mlitRestClient(MlitApiProps props) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(20));

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Ocp-Apim-Subscription-Key", props.getApiKey())
                //.defaultHeader("Accept", "application/x-protobuf")
                .build();
    }
}