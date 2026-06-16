package com.delcapital.aa.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class DigioClientConfig {

    @Value("${digio.base-url}")
    private String baseUrl;

    @Value("${digio.username}")
    private String username;

    @Value("${digio.password}")
    private String password;

    @Bean("digioWebClient")
    public WebClient digioWebClient() throws Exception {

        String credentials = Base64.getEncoder()
                .encodeToString(
                        (username + ":" + password)
                                .getBytes(StandardCharsets.UTF_8)
                );

        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + credentials
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024)
                )
                .build();
    }
}