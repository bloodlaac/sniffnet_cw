package ru.yanaeva.sniffnet_cw.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    RestClient.Builder restClientBuilder(AppProperties appProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(appProperties.getIntegration().getTimeout())
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(appProperties.getIntegration().getTimeout());

        return RestClient.builder()
            .requestFactory(requestFactory);
    }
}
